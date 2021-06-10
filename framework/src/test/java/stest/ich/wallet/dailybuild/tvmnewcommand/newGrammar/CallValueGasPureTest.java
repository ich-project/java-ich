package stest.ich.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.ich.api.GrpcAPI.AccountResourceMessage;
import org.ich.api.GrpcAPI.TransactionExtention;
import org.ich.api.WalletGrpc;
import org.ich.common.common.crypto.ECKey;
import org.ich.common.common.utils.ByteArray;
import org.ich.common.common.utils.Utils;
import org.ich.core.Wallet;
import org.ich.core.Protocol;
import org.ich.core.Protocol.TransactionInfo;
import org.ich.core.contract.SmartContractOuterClass.SmartContract;
import stest.ich.wallet.common.client.Configuration;
import stest.ich.wallet.common.client.Parameter.CommonConstant;
import stest.ich.wallet.common.client.utils.Base58;
import stest.ich.wallet.common.client.utils.PublicMethed;

@Slf4j
public class CallValueGasPureTest {

  private final String foundationKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] foundationAddress001 = PublicMethed.getFinalAddress(foundationKey001);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] contractAddress = null;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress001 = ecKey1.getAddress();
  private String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(testKey001);
  }

  @Test(enabled = true, description = "call.value.gas be pure")
  public void test01DeployContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(testAddress001, 1000_000_000L, foundationAddress001, foundationKey001,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(foundationAddress001, 100_000_000L, 0, 0,
            ByteString.copyFrom(testAddress001), foundationKey001, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(testAddress001, blockingStubFull);
    Protocol.Account info = PublicMethed.queryAccount(testKey001, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = accountResource.getEnergyUsed();
    Long beforeNetUsed = accountResource.getNetUsed();
    Long beforeFreeNetUsed = accountResource.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "./src/test/resources/soliditycode/callValueGasPure.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 0,
            10000, "0", 0, null, testKey001, testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    String param = "\"" + Base58.encode58Check(testAddress001) + "\"";
    TransactionExtention extention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "check(address)",
            param, false, 0, 1000000000L, "0", 0, testAddress001,
            testKey001, blockingStubFull);

    Assert.assertNotNull(extention);
    Assert.assertTrue(extention.hasResult());
    Assert.assertTrue(extention.getResult().getResult());

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(testKey001, blockingStubFull).getBalance();
    PublicMethed
        .sendcoin(foundationAddress001, balance, testAddress001, testKey001, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}

