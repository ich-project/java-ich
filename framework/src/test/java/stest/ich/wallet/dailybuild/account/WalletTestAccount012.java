package stest.ich.wallet.dailybuild.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.ich.api.GrpcAPI.AccountResourceMessage;
import org.ich.api.WalletGrpc;
import org.ich.common.common.crypto.ECKey;
import org.ich.common.common.utils.ByteArray;
import org.ich.common.common.utils.Utils;
import org.ich.core.Protocol.Account;
import stest.ich.wallet.common.client.Configuration;
import stest.ich.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAccount012 {
  private static final long sendAmount = 10000000000L;
  private static final long frozenAmountForIchPower = 3456789L;
  private static final long frozenAmountForNet = 7000000L;
  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witnessKey);

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenAddress = ecKey1.getAddress();
  String frozenKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(frozenKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

  }

  @Test(enabled = true, description = "Freeze balance to get tron power")
  public void test01FreezeBalanceGetIchPower() {


    final Long beforeFrozenTime = System.currentTimeMillis();
    Assert.assertTrue(PublicMethed.sendcoin(frozenAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    final Long beforeTotalIchPowerWeight = accountResource.getTotalIchPowerWeight();
    final Long beforeIchPowerLimit = accountResource.getIchPowerLimit();


    Assert.assertTrue(PublicMethed.freezeBalanceGetIchPower(frozenAddress,frozenAmountForIchPower,
        0,2,null,frozenKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetIchPower(frozenAddress,frozenAmountForNet,
        0,0,null,frozenKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long afterFrozenTime = System.currentTimeMillis();
    Account account = PublicMethed.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertEquals(account.getIchPower().getFrozenBalance(),frozenAmountForIchPower);
    Assert.assertTrue(account.getIchPower().getExpireTime() > beforeFrozenTime
        && account.getIchPower().getExpireTime() < afterFrozenTime);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTotalIchPowerWeight = accountResource.getTotalIchPowerWeight();
    Long afterIchPowerLimit = accountResource.getIchPowerLimit();
    Long afterIchPowerUsed = accountResource.getIchPowerUsed();
    Assert.assertEquals(afterTotalIchPowerWeight - beforeTotalIchPowerWeight,
        frozenAmountForIchPower / 1000000L);

    Assert.assertEquals(afterIchPowerLimit - beforeIchPowerLimit,
        frozenAmountForIchPower / 1000000L);



    Assert.assertTrue(PublicMethed.freezeBalanceGetIchPower(frozenAddress,
        6000000 - frozenAmountForIchPower,
        0,2,null,frozenKey,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    afterIchPowerLimit = accountResource.getIchPowerLimit();

    Assert.assertEquals(afterIchPowerLimit - beforeIchPowerLimit,
        6);


  }


  @Test(enabled = true,description = "Vote witness by tron power")
  public void test02VotePowerOnlyComeFromIchPower() {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    final Long beforeIchPowerUsed = accountResource.getIchPowerUsed();


    HashMap<byte[],Long> witnessMap = new HashMap<>();
    witnessMap.put(witnessAddress,frozenAmountForNet / 1000000L);
    Assert.assertFalse(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    witnessMap.put(witnessAddress,frozenAmountForIchPower / 1000000L);
    Assert.assertTrue(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterIchPowerUsed = accountResource.getIchPowerUsed();
    Assert.assertEquals(afterIchPowerUsed - beforeIchPowerUsed,
        frozenAmountForIchPower / 1000000L);

    final Long secondBeforeIchPowerUsed = afterIchPowerUsed;
    witnessMap.put(witnessAddress,(frozenAmountForIchPower / 1000000L) - 1);
    Assert.assertTrue(PublicMethed.voteWitness(frozenAddress,frozenKey,witnessMap,
        blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    afterIchPowerUsed = accountResource.getIchPowerUsed();
    Assert.assertEquals(secondBeforeIchPowerUsed - afterIchPowerUsed,
        1);


  }

  @Test(enabled = true,description = "Ich power is not allow to others")
  public void test03IchPowerIsNotAllowToOthers() {
    Assert.assertFalse(PublicMethed.freezeBalanceGetIchPower(frozenAddress,
        frozenAmountForIchPower, 0,2,
        ByteString.copyFrom(foundationAddress),frozenKey,blockingStubFull));
  }


  @Test(enabled = true,description = "Unfreeze balance for tron power")
  public void test04UnfreezeBalanceForIchPower() {
    AccountResourceMessage accountResource = PublicMethed
        .getAccountResource(foundationAddress, blockingStubFull);
    final Long beforeTotalIchPowerWeight = accountResource.getTotalIchPowerWeight();


    Assert.assertTrue(PublicMethed.unFreezeBalance(frozenAddress,frozenKey,2,
        null,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed
        .getAccountResource(frozenAddress, blockingStubFull);
    Long afterTotalIchPowerWeight = accountResource.getTotalIchPowerWeight();
    Assert.assertEquals(beforeTotalIchPowerWeight - afterTotalIchPowerWeight,
        6);

    Assert.assertEquals(accountResource.getIchPowerLimit(),0L);
    Assert.assertEquals(accountResource.getIchPowerUsed(),0L);

    Account account = PublicMethed.queryAccount(frozenAddress,blockingStubFull);
    Assert.assertEquals(account.getIchPower().getFrozenBalance(),0);


  }
  

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.unFreezeBalance(frozenAddress, frozenKey, 2, null,
        blockingStubFull);
    PublicMethed.unFreezeBalance(frozenAddress, frozenKey, 0, null,
        blockingStubFull);
    PublicMethed.freedResource(frozenAddress, frozenKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


