package org.ich.core.db;

import static org.ich.common.common.runtime.InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE;
import static org.ich.common.common.runtime.InternalTransaction.TrxType.TRX_CONTRACT_CREATION_TYPE;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;
import org.ich.common.common.runtime.InternalTransaction.TrxType;
import org.ich.common.common.runtime.ProgramResult;
import org.ich.common.common.runtime.Runtime;
import org.ich.common.common.runtime.vm.DataWord;
import org.ich.common.common.utils.DecodeUtil;
import org.ich.common.common.utils.ForkController;
import org.ich.common.common.utils.Sha256Hash;
import org.ich.common.common.utils.StringUtil;
import org.ich.common.common.utils.WalletUtil;
import org.ich.core.Constant;
import org.ich.core.capsule.AccountCapsule;
import org.ich.core.capsule.BlockCapsule;
import org.ich.core.capsule.ContractCapsule;
import org.ich.core.capsule.ReceiptCapsule;
import org.ich.core.capsule.TransactionCapsule;
import org.ich.core.exception.BalanceInsufficientException;
import org.ich.core.exception.ContractExeException;
import org.ich.core.exception.ContractValidateException;
import org.ich.core.exception.ReceiptCheckErrException;
import org.ich.core.exception.VMIllegalException;
import org.ich.core.store.*;
import org.ich.protos.Protocol.Transaction;
import org.ich.protos.Protocol.Transaction.Contract.ContractType;
import org.ich.protos.Protocol.Transaction.Result.contractResult;
import org.ich.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.ich.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@Slf4j(topic = "TransactionTrace")
public class TransactionTrace {

  private TransactionCapsule trx;

  private ReceiptCapsule receipt;

  private StoreFactory storeFactory;

  private DynamicPropertiesStore dynamicPropertiesStore;

  private ContractStore contractStore;

  private AccountStore accountStore;

  private CodeStore codeStore;

  private EnergyProcessor energyProcessor;

  private TrxType trxType;

  private long txStartTimeInMs;

  private Runtime runtime;

  private ForkController forkController;

  @Getter
  private TransactionContext transactionContext;
  @Getter
  @Setter
  private TimeResultType timeResultType = TimeResultType.NORMAL;
  @Getter
  @Setter
  private boolean netFeeForBandwidth = true;

  public TransactionTrace(TransactionCapsule trx, StoreFactory storeFactory,
      Runtime runtime) {
    this.trx = trx;
    Transaction.Contract.ContractType contractType = this.trx.getInstance().getRawData()
        .getContract(0).getType();
    switch (contractType.getNumber()) {
      case ContractType.TriggerSmartContract_VALUE:
        trxType = TRX_CONTRACT_CALL_TYPE;
        break;
      case ContractType.CreateSmartContract_VALUE:
        trxType = TRX_CONTRACT_CREATION_TYPE;
        break;
      default:
        trxType = TrxType.TRX_PRECOMPILED_TYPE;
    }
    this.storeFactory = storeFactory;
    this.dynamicPropertiesStore = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
    this.contractStore = storeFactory.getChainBaseManager().getContractStore();
    this.codeStore = storeFactory.getChainBaseManager().getCodeStore();
    this.accountStore = storeFactory.getChainBaseManager().getAccountStore();

    this.receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);
    this.energyProcessor = new EnergyProcessor(dynamicPropertiesStore, accountStore);
    this.runtime = runtime;
    this.forkController = new ForkController();
    forkController.init(storeFactory.getChainBaseManager());
  }

  public TransactionCapsule getTrx() {
    return trx;
  }

  private boolean needVM() {
    return this.trxType == TRX_CONTRACT_CALL_TYPE
        || this.trxType == TRX_CONTRACT_CREATION_TYPE;
  }

  public void init(BlockCapsule blockCap) {
    init(blockCap, false);
  }

  //pre transaction check
  public void init(BlockCapsule blockCap, boolean eventPluginLoaded) {
    txStartTimeInMs = System.currentTimeMillis();
    transactionContext = new TransactionContext(blockCap, trx, storeFactory, false,
        eventPluginLoaded);
  }

  public void checkIsConstant() throws ContractValidateException, VMIllegalException {
    if (dynamicPropertiesStore.getAllowTvmConstantinople() == 1) {
      return;
    }
    TriggerSmartContract triggerContractFromTransaction = ContractCapsule
        .getTriggerContractFromTransaction(this.getTrx().getInstance());
    if (TRX_CONTRACT_CALL_TYPE == this.trxType) {
      ContractCapsule contract = contractStore
          .get(triggerContractFromTransaction.getContractAddress().toByteArray());
      if (contract == null) {
        logger.info("contract: {} is not in contract store", StringUtil
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray()));
        throw new ContractValidateException("contract: " + StringUtil
            .encode58Check(triggerContractFromTransaction.getContractAddress().toByteArray())
            + " is not in contract store");
      }
      ABI abi = contract.getInstance().getAbi();
      if (WalletUtil.isConstant(abi, triggerContractFromTransaction)) {
        throw new VMIllegalException("cannot call constant method");
      }
    }
  }

  //set bill
  public void setBill(long energyUsage) {
    if (energyUsage < 0) {
      energyUsage = 0L;
    }
    receipt.setEnergyUsageTotal(energyUsage);
  }

  //set net bill
  public void setNetBill(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
  }

  public void setNetBillForCreateNewAccount(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
    setNetFeeForBandwidth(false);
  }

  public void addNetBill(long netFee) {
    receipt.addNetFee(netFee);
  }

  public void exec()
      throws ContractExeException, ContractValidateException, VMIllegalException {
    /*  VM execute  */
    runtime.execute(transactionContext);
    setBill(transactionContext.getProgramResult().getEnergyUsed());

//    if (TrxType.TRX_PRECOMPILED_TYPE != trxType) {
//      if (contractResult.OUT_OF_TIME
//          .equals(receipt.getResult())) {
//        setTimeResultType(TimeResultType.OUT_OF_TIME);
//      } else if (System.currentTimeMillis() - txStartTimeInMs
//          > CommonParameter.getInstance()
//          .getLongRunningTime()) {
//        setTimeResultType(TimeResultType.LONG_RUNNING);
//      }
//    }
  }

  public void saveEnergyLeftOfOrigin(long energyLeft) {
    receipt.setOriginEnergyLeft(energyLeft);
  }

  public void saveEnergyLeftOfCaller(long energyLeft) {
    receipt.setCallerEnergyLeft(energyLeft);
  }

  public void finalization() throws ContractExeException {
    try {
      pay();
    } catch (BalanceInsufficientException e) {
      throw new ContractExeException(e.getMessage());
    }
    if (StringUtils.isEmpty(transactionContext.getProgramResult().getRuntimeError())) {
      for (DataWord contract : transactionContext.getProgramResult().getDeleteAccounts()) {
        deleteContract(convertToTronAddress((contract.getLast20Bytes())));
      }
    }
  }

  /**
   * pay actually bill(include ENERGY and storage).
   */
  public void pay() throws BalanceInsufficientException {
    byte[] originAccount;
    byte[] callerAccount;
    long percent = 0;
    long originEnergyLimit = 0;
    switch (trxType) {
      case TRX_CONTRACT_CREATION_TYPE:
        callerAccount = TransactionCapsule.getOwner(trx.getInstance().getRawData().getContract(0));
        originAccount = callerAccount;
        break;
      case TRX_CONTRACT_CALL_TYPE:
        TriggerSmartContract callContract = ContractCapsule
            .getTriggerContractFromTransaction(trx.getInstance());
        ContractCapsule contractCapsule =
            contractStore.get(callContract.getContractAddress().toByteArray());

        callerAccount = callContract.getOwnerAddress().toByteArray();
        originAccount = contractCapsule.getOriginAddress();
        percent = Math
            .max(Constant.ONE_HUNDRED - contractCapsule.getConsumeUserResourcePercent(), 0);
        percent = Math.min(percent, Constant.ONE_HUNDRED);
        originEnergyLimit = contractCapsule.getOriginEnergyLimit();
        break;
      default:
        return;
    }

    // originAccount Percent = 30%
    AccountCapsule origin = accountStore.get(originAccount);
    AccountCapsule caller = accountStore.get(callerAccount);
    receipt.payEnergyBill(
        dynamicPropertiesStore, accountStore, forkController,
        origin,
        caller,
        percent, originEnergyLimit,
        energyProcessor,
        EnergyProcessor.getHeadSlot(dynamicPropertiesStore));
  }

  public boolean checkNeedRetry() {
    if (!needVM()) {
      return false;
    }
    return trx.getContractRet() != contractResult.OUT_OF_TIME && receipt.getResult()
        == contractResult.OUT_OF_TIME;
  }

  public void check() throws ReceiptCheckErrException {
    if (!needVM()) {
      return;
    }
    if (Objects.isNull(trx.getContractRet())) {
      throw new ReceiptCheckErrException("null resultCode");
    }
    if (!trx.getContractRet().equals(receipt.getResult())) {
      logger.info(
          "this tx id: {}, the resultCode in received block: {}, the resultCode in self: {}",
          Hex.toHexString(trx.getTransactionId().getBytes()), trx.getContractRet(),
          receipt.getResult());
      throw new ReceiptCheckErrException("Different resultCode");
    }
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public void setResult() {
    if (!needVM()) {
      return;
    }
    receipt.setResult(transactionContext.getProgramResult().getResultCode());
  }

  public String getRuntimeError() {
    return transactionContext.getProgramResult().getRuntimeError();
  }

  public ProgramResult getRuntimeResult() {
    return transactionContext.getProgramResult();
  }

  public Runtime getRuntime() {
    return runtime;
  }

  public void deleteContract(byte[] address) {
    codeStore.delete(address);
    accountStore.delete(address);
    contractStore.delete(address);
  }

  public static byte[] convertToTronAddress(byte[] address) {
    if (address.length == 20) {
      byte[] newAddress = new byte[21];
      byte[] temp = new byte[]{DecodeUtil.addressPreFixByte};
      System.arraycopy(temp, 0, newAddress, 0, temp.length);
      System.arraycopy(address, 0, newAddress, temp.length, address.length);
      address = newAddress;
    }
    return address;
  }

  public enum TimeResultType {
    NORMAL,
    LONG_RUNNING,
    OUT_OF_TIME
  }
}
