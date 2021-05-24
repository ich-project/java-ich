package org.ich.core.pbft;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ich.common.common.application.TronApplicationContext;
import org.ich.common.common.crypto.ECKey;
import org.ich.common.common.utils.FileUtil;
import org.ich.common.common.utils.Sha256Hash;
import org.ich.common.common.utils.Utils;
import org.ich.core.ChainBaseManager;
import org.ich.core.Constant;
import org.ich.core.capsule.BlockCapsule;
import org.ich.core.config.DefaultConfig;
import org.ich.core.config.args.Args;
import org.ich.core.db.BlockGenerate;
import org.ich.core.db.CommonDataBase;
import org.ich.core.db.Manager;
import org.ich.core.db2.ISession;
import org.ich.core.exception.HeaderNotFound;
import org.ich.core.services.interfaceOnPBFT.http.PBFT.HttpApiOnPBFTService;
import org.ich.core.store.DynamicPropertiesStore;

@Slf4j
public class PbftApiTest extends BlockGenerate {

  private static Manager dbManager;
  private static TronApplicationContext context;
  private static String dbPath = "output_pbftAPI_test";

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
    setManager(dbManager);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void pbftapi() throws IOException, InterruptedException, HeaderNotFound {
    ChainBaseManager chainBaseManager = dbManager.getChainBaseManager();
    DynamicPropertiesStore dynamicPropertiesStore = chainBaseManager.getDynamicPropertiesStore();
    CommonDataBase commonDataBase = chainBaseManager.getCommonDataBase();

    for (int i = 1; i <= 10; i++) {
      try (ISession tmpSession = dbManager.getRevokingStore().buildSession()) {
        BlockCapsule blockCapsule = createTestBlockCapsule(
            dynamicPropertiesStore.getLatestBlockHeaderTimestamp() + 3000L,
            dbManager.getHeadBlockNum() + 1, dynamicPropertiesStore.getLatestBlockHeaderHash());
        dynamicPropertiesStore.saveLatestBlockHeaderNumber(blockCapsule.getNum());
        dynamicPropertiesStore.saveLatestBlockHeaderTimestamp(blockCapsule.getTimeStamp());
        dbManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);
        tmpSession.commit();
      }
    }

    Assert.assertTrue(dynamicPropertiesStore.getLatestBlockHeaderNumber() >= 10);
    commonDataBase.saveLatestPbftBlockNum(6);
    HttpApiOnPBFTService httpApiOnPBFTService = context.getBean(HttpApiOnPBFTService.class);
    httpApiOnPBFTService.start();
    CloseableHttpResponse response = null;
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet("http://127.0.0.1:8092/walletpbft/getnowblock");
      response = httpClient.execute(httpGet);
      String responseString = EntityUtils.toString(response.getEntity());
      JSONObject jsonObject = JSON.parseObject(responseString);
      long num = jsonObject.getJSONObject("block_header").getJSONObject("raw_data")
          .getLongValue("number");
      Assert.assertEquals(commonDataBase.getLatestPbftBlockNum(), num);
      response.close();
    }
    httpApiOnPBFTService.stop();
  }

  private BlockCapsule createTestBlockCapsule(long time, long number, Sha256Hash hash) {
    ECKey ecKey = new ECKey(Utils.getRandom());
    ByteString address = ByteString.copyFrom(ecKey.getAddress());

    BlockCapsule blockCapsule = new BlockCapsule(number, hash, time, address);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    return blockCapsule;
  }
}
