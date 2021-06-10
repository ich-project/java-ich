package org.ich.core.db;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ich.common.common.application.IchApplicationContext;
import org.ich.common.common.utils.ByteArray;
import org.ich.common.common.utils.FileUtil;
import org.ich.core.Constant;
import org.ich.core.capsule.TransactionInfoCapsule;
import org.ich.core.config.DefaultConfig;
import org.ich.core.config.args.Args;
import org.ich.core.exception.BadItemException;
import org.ich.core.store.TransactionHistoryStore;

public class TransactionHistoryTest {

  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);
  private static String dbPath = "output_TransactionHistoryStore_test";
  private static String dbDirectory = "db_TransactionHistoryStore_test";
  private static String indexDirectory = "index_TransactionHistoryStore_test";
  private static IchApplicationContext context;
  private static TransactionHistoryStore transactionHistoryStore;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
    context = new IchApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    transactionHistoryStore = context.getBean(TransactionHistoryStore.class);
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();

    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);
    transactionHistoryStore.put(transactionId, transactionInfoCapsule);
  }

  @Test
  public void get() throws BadItemException {
    //test get and has Method
    TransactionInfoCapsule resultCapsule = transactionHistoryStore.get(transactionId);
    Assert.assertEquals(1000L, resultCapsule.getFee());
    Assert.assertEquals(100L, resultCapsule.getBlockNumber());
    Assert.assertEquals(200L, resultCapsule.getBlockTimeStamp());
    Assert.assertEquals(ByteArray.toHexString(transactionId),
        ByteArray.toHexString(resultCapsule.getId()));
  }
}