package org.ich.core.capsule;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.ich.common.common.parameter.CommonParameter;
import org.ich.common.common.utils.Commons;
import org.ich.common.common.utils.ForkController;
import org.ich.common.common.utils.Sha256Hash;
import org.ich.common.common.utils.StringUtil;
import org.ich.core.Constant;
import org.ich.core.config.Parameter.ForkBlockVersionEnum;
import org.ich.core.db.EnergyProcessor;
import org.ich.core.exception.BalanceInsufficientException;
import org.ich.core.store.AccountStore;
import org.ich.core.store.DynamicPropertiesStore;
import org.ich.protos.Protocol.ResourceReceipt;
import org.ich.protos.Protocol.Transaction.Result.contractResult;

public class ReceiptCapsule {

  private ResourceReceipt receipt;
  @Getter
  @Setter
  private long multiSignFee;

  /**
   * 执行交易之前合约部署账户上可用的能量
   */
  @Setter
  private long originEnergyLeft;

  /**
   * 执行交易之前调用者账户上可用的能量
   */
  @Setter
  private long callerEnergyLeft;

  private Sha256Hash receiptAddress;

  public ReceiptCapsule(ResourceReceipt data, Sha256Hash receiptAddress) {
    this.receipt = data;
    this.receiptAddress = receiptAddress;
  }

  public ReceiptCapsule(Sha256Hash receiptAddress) {
    this.receipt = ResourceReceipt.newBuilder().build();
    this.receiptAddress = receiptAddress;
  }

  public static ResourceReceipt copyReceipt(ReceiptCapsule origin) {
    return origin.getReceipt().toBuilder().build();
  }

  public static boolean checkForEnergyLimit(DynamicPropertiesStore ds) {
    long blockNum = ds.getLatestBlockHeaderNumber();
    return blockNum >= CommonParameter.getInstance()
        .getBlockNumForEnergyLimit();
  }

  public ResourceReceipt getReceipt() {
    return this.receipt;
  }

  public void setReceipt(ResourceReceipt receipt) {
    this.receipt = receipt;
  }

  public Sha256Hash getReceiptAddress() {
    return this.receiptAddress;
  }

  public void addNetFee(long netFee) {
    this.receipt = this.receipt.toBuilder().setNetFee(getNetFee() + netFee).build();
  }

  public long getEnergyUsage() {
    return this.receipt.getEnergyUsage();
  }

  public void setEnergyUsage(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setEnergyUsage(energyUsage).build();
  }

  public long getEnergyFee() {
    return this.receipt.getEnergyFee();
  }

  public void setEnergyFee(long energyFee) {
    this.receipt = this.receipt.toBuilder().setEnergyFee(energyFee).build();
  }

  public long getOriginEnergyUsage() {
    return this.receipt.getOriginEnergyUsage();
  }

  public void setOriginEnergyUsage(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setOriginEnergyUsage(energyUsage).build();
  }

  public long getEnergyUsageTotal() {
    return this.receipt.getEnergyUsageTotal();
  }

  public void setEnergyUsageTotal(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setEnergyUsageTotal(energyUsage).build();
  }

  public long getNetUsage() {
    return this.receipt.getNetUsage();
  }

  public void setNetUsage(long netUsage) {
    this.receipt = this.receipt.toBuilder().setNetUsage(netUsage).build();
  }

  public long getNetFee() {
    return this.receipt.getNetFee();
  }

  public void setNetFee(long netFee) {
    this.receipt = this.receipt.toBuilder().setNetFee(netFee).build();
  }

  /**
   * payEnergyBill pay receipt energy bill by energy processor.
   */
  public void payEnergyBill(DynamicPropertiesStore dynamicPropertiesStore,
      AccountStore accountStore, ForkController forkController, AccountCapsule origin,
      AccountCapsule caller,
      long percent, long originEnergyLimit, EnergyProcessor energyProcessor, long now)
      throws BalanceInsufficientException {
    if (receipt.getEnergyUsageTotal() <= 0) {
      return;
    }

    if (Objects.isNull(origin) && dynamicPropertiesStore.getAllowTvmConstantinople() == 1) {
      payEnergyBill(dynamicPropertiesStore, accountStore, forkController, caller,
          receipt.getEnergyUsageTotal(), receipt.getResult(), energyProcessor, now);
      return;
    }

    if (caller.getAddress().equals(origin.getAddress())) {
      payEnergyBill(dynamicPropertiesStore, accountStore, forkController, caller,
          receipt.getEnergyUsageTotal(), receipt.getResult(), energyProcessor, now);
    } else {
      long originUsage = Math.multiplyExact(receipt.getEnergyUsageTotal(), percent) / 100;
      originUsage = getOriginUsage(dynamicPropertiesStore, origin, originEnergyLimit,
          energyProcessor,
          originUsage);

      long callerUsage = receipt.getEnergyUsageTotal() - originUsage;
      energyProcessor.useEnergy(origin, originUsage, now);
      this.setOriginEnergyUsage(originUsage);
      payEnergyBill(dynamicPropertiesStore, accountStore, forkController,
          caller, callerUsage, receipt.getResult(), energyProcessor, now);
    }
  }

  private long getOriginUsage(DynamicPropertiesStore dynamicPropertiesStore, AccountCapsule origin,
      long originEnergyLimit,
      EnergyProcessor energyProcessor, long originUsage) {

    if (dynamicPropertiesStore.getAllowTvmFreeze() == 1) {
      return Math.min(originUsage, Math.min(originEnergyLeft, originEnergyLimit));
    }

    if (checkForEnergyLimit(dynamicPropertiesStore)) {
      return Math.min(originUsage,
          Math.min(energyProcessor.getAccountLeftEnergyFromFreeze(origin), originEnergyLimit));
    }
    return Math.min(originUsage, energyProcessor.getAccountLeftEnergyFromFreeze(origin));
  }

  private void payEnergyBill(
      DynamicPropertiesStore dynamicPropertiesStore, AccountStore accountStore,
      ForkController forkController,
      AccountCapsule account,
      long usage,
      contractResult contractResult,
      EnergyProcessor energyProcessor,
      long now) throws BalanceInsufficientException {
    long accountEnergyLeft;
    if (dynamicPropertiesStore.getAllowTvmFreeze() == 1) {
      accountEnergyLeft = callerEnergyLeft;
    } else {
      accountEnergyLeft = energyProcessor.getAccountLeftEnergyFromFreeze(account);
    }
    if (accountEnergyLeft >= usage) {
      energyProcessor.useEnergy(account, usage, now);
      this.setEnergyUsage(usage);
    } else {
      energyProcessor.useEnergy(account, accountEnergyLeft, now);

      if (forkController.pass(ForkBlockVersionEnum.VERSION_3_6_5) &&
          dynamicPropertiesStore.getAllowAdaptiveEnergy() == 1) {
        long blockEnergyUsage =
            dynamicPropertiesStore.getBlockEnergyUsage() + (usage - accountEnergyLeft);
        dynamicPropertiesStore.saveBlockEnergyUsage(blockEnergyUsage);
      }

      long sunPerEnergy = Constant.SUN_PER_ENERGY;
      long dynamicEnergyFee = dynamicPropertiesStore.getEnergyFee();
      if (dynamicEnergyFee > 0) {
        sunPerEnergy = dynamicEnergyFee;
      }
      long energyFee =
          (usage - accountEnergyLeft) * sunPerEnergy;
      this.setEnergyUsage(accountEnergyLeft);
      this.setEnergyFee(energyFee);
      long balance = account.getBalance();
      if (balance < energyFee) {
        throw new BalanceInsufficientException(
            StringUtil.createReadableString(account.createDbKey()) + " insufficient balance");
      }
      account.setBalance(balance - energyFee);

      if (dynamicPropertiesStore.supportTransactionFeePool() &&
          !contractResult.equals(contractResult.OUT_OF_TIME)) {
        dynamicPropertiesStore.addTransactionFeePool(energyFee);
      } else if (dynamicPropertiesStore.supportBlackHoleOptimization()) {
        dynamicPropertiesStore.burnTrx(energyFee);
      } else {
        //send to blackHole
        Commons.adjustBalance(accountStore, accountStore.getBlackhole(),
            energyFee);
      }

    }

    accountStore.put(account.getAddress().toByteArray(), account);
  }

  public contractResult getResult() {
    return this.receipt.getResult();
  }

  public void setResult(contractResult success) {
    this.receipt = receipt.toBuilder().setResult(success).build();
  }
}
