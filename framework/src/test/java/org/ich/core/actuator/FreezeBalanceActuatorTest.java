package org.ich.core.actuator;

import static junit.framework.TestCase.fail;
import static org.ich.core.config.Parameter.ChainConstant.TRANSFER_FEE;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ich.common.common.application.IchApplicationContext;
import org.ich.common.common.utils.ByteArray;
import org.ich.common.common.utils.FileUtil;
import org.ich.core.ChainBaseManager;
import org.ich.core.Constant;
import org.ich.core.Wallet;
import org.ich.core.capsule.AccountCapsule;
import org.ich.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.ich.core.capsule.DelegatedResourceCapsule;
import org.ich.core.capsule.TransactionResultCapsule;
import org.ich.core.config.DefaultConfig;
import org.ich.core.config.Parameter.ChainConstant;
import org.ich.core.config.args.Args;
import org.ich.core.db.Manager;
import org.ich.core.exception.ContractExeException;
import org.ich.core.exception.ContractValidateException;
import org.ich.core.Protocol.AccountType;
import org.ich.core.Protocol.Transaction.Result.code;
import org.ich.core.contract.AssetIssueContractOuterClass;
import org.ich.core.contract.BalanceContract.FreezeBalanceContract;
import org.ich.core.contract.Common.ResourceCode;

@Slf4j
public class FreezeBalanceActuatorTest {

  private static final String dbPath = "output_freeze_balance_test";
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static Manager dbManager;
  private static IchApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new IchApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule receiverCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
            AccountType.Normal,
            initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);
  }

  private Any getContractForBandwidth(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  private Any getContractForCpu(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .setResource(ResourceCode.ENERGY)
            .build());
  }


  private Any getContractForIchPower(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .setResource(ResourceCode.ICH_POWER)
            .build());
  }

  private Any getDelegatedContractForBandwidth(String ownerAddress, String receiverAddress,
      long frozenBalance,
      long duration) {
    return Any.pack(
        FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  private Any getDelegatedContractForCpu(String ownerAddress, String receiverAddress,
      long frozenBalance,
      long duration) {
    return Any.pack(
        FreezeBalanceContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(receiverAddress)))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .setResource(ResourceCode.ENERGY)
            .build());
  }

  @Test
  public void testFreezeBalanceForBandwidth() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(owner.getFrozenBalance(), frozenBalance);
      Assert.assertEquals(frozenBalance, owner.getIchPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testFreezeBalanceForEnergy() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpu(OWNER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getFrozenBalance());
      Assert.assertEquals(frozenBalance, owner.getEnergyFrozenBalance());
      Assert.assertEquals(frozenBalance, owner.getIchPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testFreezeDelegatedBalanceForBandwidthWithContractAddress() {
    AccountCapsule receiverCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("receiver"),
            ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)),
            AccountType.Contract,
            initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(), "Do not allow delegate resources to contract addresses");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testFreezeDelegatedBalanceForBandwidth() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForBandwidth(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getFrozenBalance());
      Assert.assertEquals(frozenBalance, owner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, owner.getIchPower());

      AccountCapsule receiver =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(frozenBalance, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiver.getIchPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule
              .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
                  ByteArray.fromHexString(RECEIVER_ADDRESS)));

      Assert.assertEquals(frozenBalance, delegatedResourceCapsule.getFrozenBalanceForBandwidth());
      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore + frozenBalance / 1000_000L, totalNetWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(1, delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert
          .assertEquals(1,
              delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));


    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testFreezeDelegatedBalanceForCpuSameNameTokenActive() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getFrozenBalance());
      Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, owner.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(frozenBalance, owner.getIchPower());

      AccountCapsule receiver =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiver.getIchPower());

      DelegatedResourceCapsule delegatedResourceCapsule = dbManager.getDelegatedResourceStore()
          .get(DelegatedResourceCapsule
              .createDbKey(ByteArray.fromHexString(OWNER_ADDRESS),
                  ByteArray.fromHexString(RECEIVER_ADDRESS)));

      Assert.assertEquals(0L, delegatedResourceCapsule.getFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance, delegatedResourceCapsule.getFrozenBalanceForEnergy());

      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore + frozenBalance / 1000_000L,
          totalEnergyWeightAfter);

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(1, delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS))));

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert
          .assertEquals(0, delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert
          .assertEquals(1,
              delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());
      Assert.assertEquals(true,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList()
              .contains(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS))));

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testFreezeDelegatedBalanceForCpuSameNameTokenClose() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(0);
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager()).setAny(
        getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));
      Assert.assertEquals(owner.getBalance(), initBalance - frozenBalance
          - TRANSFER_FEE);
      Assert.assertEquals(0L, owner.getFrozenBalance());
      Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, owner.getDelegatedFrozenBalanceForEnergy());

      AccountCapsule receiver =
          dbManager.getAccountStore().get(ByteArray.fromHexString(RECEIVER_ADDRESS));
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(0L, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiver.getIchPower());

      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore + frozenBalance / 1000_000L,
          totalEnergyWeightAfter);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void freezeLessThanZero() {
    long frozenBalance = -1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be positive", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void freezeMoreThanBalance() {
    long frozenBalance = 11_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be less than accountBalance", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS_INVALID, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);

      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void invalidOwnerAccount() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ACCOUNT_INVALID, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void durationLessThanMin() {
    long frozenBalance = 1_000_000_000L;
    long duration = 2;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
      long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenDuration must be less than " + maxFrozenTime + " days "
          + "and more than " + minFrozenTime + " days", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void durationMoreThanMax() {
    long frozenBalance = 1_000_000_000L;
    long duration = 4;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      long minFrozenTime = dbManager.getDynamicPropertiesStore().getMinFrozenTime();
      long maxFrozenTime = dbManager.getDynamicPropertiesStore().getMaxFrozenTime();
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenDuration must be less than " + maxFrozenTime + " days "
          + "and more than " + minFrozenTime + " days", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void lessThan1TrxTest() {
    long frozenBalance = 1;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenBalance must be more than 1TRX", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void frozenNumTest() {
    AccountCapsule account = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    account.setFrozen(1_000L, 1_000_000_000L);
    account.setFrozen(1_000_000L, 1_000_000_000L);
    dbManager.getAccountStore().put(account.getAddress().toByteArray(), account);

    long frozenBalance = 20_000_000L;
    long duration = 3L;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("frozenCount must be 0 or 1", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  //@Test
  public void moreThanFrozenNumber() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      long maxFrozenNumber = ChainConstant.MAX_FROZEN_NUMBER;
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("max frozen number is: " + maxFrozenNumber, e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void commonErrorCheck() {
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [FreezeBalanceContract],real type[");
    actuatorTest.invalidContractType();

    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    actuatorTest.setContract(getContractForBandwidth(OWNER_ADDRESS, frozenBalance, duration));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }


  @Test
  public void testFreezeBalanceForEnergyWithoutOldIchPowerAfterNewResourceModel() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    chainBaseManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractForCpu(OWNER_ADDRESS, frozenBalance, duration));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(-1L, owner.getInstance().getOldIchPower());
      Assert.assertEquals(0L, owner.getAllIchPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testFreezeBalanceForEnergyWithOldIchPowerAfterNewResourceModel() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    chainBaseManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractForCpu(OWNER_ADDRESS, frozenBalance, duration));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    AccountCapsule owner =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setFrozenForEnergy(100L,0L);
    chainBaseManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS),owner);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(100L, owner.getInstance().getOldIchPower());
      Assert.assertEquals(100L, owner.getAllIchPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testFreezeBalanceForIchPowerWithOldIchPowerAfterNewResourceModel() {
    long frozenBalance = 1_000_000_000L;
    long duration = 3;
    FreezeBalanceActuator actuator = new FreezeBalanceActuator();
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    chainBaseManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    actuator.setChainBaseManager(chainBaseManager)
        .setAny(getContractForIchPower(OWNER_ADDRESS, frozenBalance, duration));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    AccountCapsule owner =
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    owner.setFrozenForEnergy(100L,0L);
    chainBaseManager.getAccountStore().put(ByteArray.fromHexString(OWNER_ADDRESS),owner);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(100L, owner.getInstance().getOldIchPower());
      Assert.assertEquals(frozenBalance + 100L, owner.getAllIchPower());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


}
