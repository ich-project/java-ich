package org.ich.common.common.runtime.vm;

import static org.ich.core.db.TransactionTrace.convertToIchAddress;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import lombok.extern.slf4j.Slf4j;
import org.ich.common.common.application.Application;
import org.ich.common.common.application.ApplicationFactory;
import org.ich.common.common.application.IchApplicationContext;
import org.ich.common.common.utils.ByteArray;
import org.ich.common.common.utils.ByteUtil;
import org.ich.common.common.utils.FileUtil;
import org.ich.common.common.utils.StringUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.ich.common.common.runtime.ProgramResult;
import org.ich.core.Constant;
import org.ich.core.Wallet;
import org.ich.core.actuator.FreezeBalanceActuator;
import org.ich.core.capsule.AccountCapsule;
import org.ich.core.capsule.ProposalCapsule;
import org.ich.core.capsule.TransactionResultCapsule;
import org.ich.core.capsule.WitnessCapsule;
import org.ich.core.config.DefaultConfig;
import org.ich.core.config.args.Args;
import org.ich.core.db.Manager;
import org.ich.core.exception.ContractExeException;
import org.ich.core.exception.ContractValidateException;
import org.ich.core.exception.ItemNotFoundException;
import org.ich.core.store.StoreFactory;
import org.ich.core.vm.PrecompiledContracts;
import org.ich.core.vm.PrecompiledContracts.PrecompiledContract;
import org.ich.core.vm.repository.Repository;
import org.ich.core.vm.repository.RepositoryImpl;
import org.ich.core.Protocol.AccountType;
import org.ich.core.Protocol.Proposal.State;
import org.ich.core.contract.BalanceContract.FreezeBalanceContract;

@Slf4j
public class PrecompiledContractsTest {

  // common
  private static final DataWord voteContractAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010001");
  private static final DataWord withdrawBalanceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010004");
  private static final DataWord proposalApproveAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010005");
  private static final DataWord proposalCreateAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010006");
  private static final DataWord proposalDeleteAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010007");
  private static final DataWord convertFromIchBytesAddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010008");
  private static final DataWord convertFromIchBase58AddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010009");
  private static final String dbPath = "output_PrecompiledContracts_test";
  private static final String ACCOUNT_NAME = "account";
  private static final String OWNER_ADDRESS;
  private static final String WITNESS_NAME = "witness";
  private static final String WITNESS_ADDRESS;
  private static final String WITNESS_ADDRESS_BASE = "548794500882809695a8a687866e76d4271a1abc";
  private static final String URL = "https://tron.network";
  // withdraw
  private static final long initBalance = 10_000_000_000L;
  private static final long allowance = 32_000_000L;
  private static IchApplicationContext context;
  private static Application appT;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new IchApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + WITNESS_ADDRESS_BASE;

  }


  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
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
  public void createCapsule() {
    // witness: witnessCapsule
    WitnessCapsule witnessCapsule =
        new WitnessCapsule(
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            10L,
            URL);
    // witness: AccountCapsule
    AccountCapsule witnessAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(WITNESS_NAME),
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            AccountType.Normal,
            initBalance);
    // some normal account
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            10_000_000_000_000L);

    dbManager.getAccountStore()
        .put(witnessAccountCapsule.getAddress().toByteArray(), witnessAccountCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }

  private Any getFreezeContract(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        FreezeBalanceContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  private PrecompiledContract createPrecompiledContract(DataWord addr, String ownerAddress) {
    PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
    contract.setCallerAddress(convertToIchAddress(Hex.decode(ownerAddress)));
    contract.setRepository(RepositoryImpl.createRoot(StoreFactory.getInstance()));
    ProgramResult programResult = new ProgramResult();
    contract.setResult(programResult);
    return contract;
  }

  //@Test
  public void voteWitnessNativeTest()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      InstantiationException, ContractValidateException, ContractExeException {
    PrecompiledContract contract = createPrecompiledContract(voteContractAddr, OWNER_ADDRESS);
    Repository deposit = RepositoryImpl.createRoot(StoreFactory.getInstance());
    byte[] witnessAddressBytes = new byte[32];
    byte[] witnessAddressBytes21 = Hex.decode(WITNESS_ADDRESS);
    System.arraycopy(witnessAddressBytes21, 0, witnessAddressBytes,
        witnessAddressBytes.length - witnessAddressBytes21.length,
        witnessAddressBytes21.length);

    DataWord voteCount = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000000001");
    byte[] voteCountBytes = voteCount.getData();
    byte[] data = new byte[witnessAddressBytes.length + voteCountBytes.length];
    System.arraycopy(witnessAddressBytes, 0, data, 0, witnessAddressBytes.length);
    System.arraycopy(voteCountBytes, 0, data, witnessAddressBytes.length,
        voteCountBytes.length);

    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    Any freezeContract = getFreezeContract(OWNER_ADDRESS, frozenBalance, duration);
    Constructor<FreezeBalanceActuator> constructor =
        FreezeBalanceActuator.class
            .getDeclaredConstructor(Any.class, dbManager.getClass());
    constructor.setAccessible(true);
    FreezeBalanceActuator freezeBalanceActuator = constructor
        .newInstance(freezeContract, dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    freezeBalanceActuator.validate();
    freezeBalanceActuator.execute(ret);
    contract.setRepository(deposit);
    Boolean result = contract.execute(data).getLeft();
    deposit.commit();
    Assert.assertEquals(1,
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteCount());
    Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteAddress().toByteArray());
    Assert.assertEquals(true, result);
  }

  //@Test
  public void proposalTest() {

    try {
      /*
       *  create proposal Test
       */
      DataWord key = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000000");
      // 1000000 == 0xF4240
      DataWord value = new DataWord(
          "00000000000000000000000000000000000000000000000000000000000F4240");
      byte[] data4Create = new byte[64];
      System.arraycopy(key.getData(), 0, data4Create, 0, key.getData().length);
      System
          .arraycopy(value.getData(), 0, data4Create,
              key.getData().length, value.getData().length);
      PrecompiledContract createContract = createPrecompiledContract(proposalCreateAddr,
          WITNESS_ADDRESS);

      Assert.assertEquals(0, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      ProposalCapsule proposalCapsule;
      Repository deposit1 = RepositoryImpl.createRoot(StoreFactory.getInstance());
      createContract.setRepository(deposit1);
      byte[] idBytes = createContract.execute(data4Create).getRight();
      long id = ByteUtil.byteArrayToLong(idBytes);
      deposit1.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalCapsule);
      Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      Assert.assertEquals(0, proposalCapsule.getApprovals().size());
      Assert.assertEquals(1000000, proposalCapsule.getCreateTime());
      Assert.assertEquals(261200000, proposalCapsule.getExpirationTime()
      ); // 2000000 + 3 * 4 * 21600000



      /*
       *  approve proposal Test
       */

      byte[] data4Approve = new byte[64];
      DataWord isApprove = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000001");
      System.arraycopy(idBytes, 0, data4Approve, 0, idBytes.length);
      System.arraycopy(isApprove.getData(), 0, data4Approve, idBytes.length,
          isApprove.getData().length);
      PrecompiledContract approveContract = createPrecompiledContract(proposalApproveAddr,
          WITNESS_ADDRESS);
      Repository deposit2 = RepositoryImpl.createRoot(StoreFactory.getInstance());
      approveContract.setRepository(deposit2);
      approveContract.execute(data4Approve);
      deposit2.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(1, proposalCapsule.getApprovals().size());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
          proposalCapsule.getApprovals().get(0));

      /*
       *  delete proposal Test
       */
      PrecompiledContract deleteContract = createPrecompiledContract(proposalDeleteAddr,
          WITNESS_ADDRESS);
      Repository deposit3 = RepositoryImpl.createRoot(StoreFactory.getInstance());
      deleteContract.setRepository(deposit3);
      deleteContract.execute(idBytes);
      deposit3.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(State.CANCELED, proposalCapsule.getState());

    } catch (ItemNotFoundException e) {
      Assert.fail();
    }
  }

  @Test
  public void convertFromIchBytesAddressNativeTest() {
  }

  //@Test
  public void convertFromIchBase58AddressNative() {
    // 27WnTihwXsqCqpiNedWvtKCZHsLjDt4Hfmf  TestNet address
    DataWord word1 = new DataWord(
        "3237576e54696877587371437170694e65645776744b435a48734c6a44743448");
    DataWord word2 = new DataWord(
        "666d660000000000000000000000000000000000000000000000000000000000");

    byte[] data = new byte[35];
    System.arraycopy(word1.getData(), 0, data, 0, word1.getData().length);
    System.arraycopy(Arrays.copyOfRange(word2.getData(), 0, 3), 0,
        data, word1.getData().length, 3);
    PrecompiledContract contract = createPrecompiledContract(convertFromIchBase58AddressAddr,
        WITNESS_ADDRESS);

    byte[] solidityAddress = contract.execute(data).getRight();
    Assert.assertArrayEquals(solidityAddress,
        new DataWord(Hex.decode(WITNESS_ADDRESS_BASE)).getData());
  }

}
