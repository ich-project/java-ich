package org.ich.core.db;

import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ich.common.common.application.Application;
import org.ich.common.common.application.ApplicationFactory;
import org.ich.common.common.application.IchApplicationContext;
import org.ich.common.common.utils.FileUtil;
import org.ich.core.Constant;
import org.ich.core.Wallet;
import org.ich.core.capsule.BytesCapsule;
import org.ich.core.config.DefaultConfig;
import org.ich.core.config.args.Args;
import org.ich.core.store.NullifierStore;

public class NullifierStoreTest {

  private static final byte[] NULLIFIER_ONE = randomBytes(32);
  private static final byte[] NULLIFIER_TWO = randomBytes(32);
  private static final byte[] TRX_TWO = randomBytes(32);
  private static final byte[] TRX_TWO_NEW = randomBytes(32);
  public static Application AppT;
  private static NullifierStore nullifierStore;
  private static String dbPath = "output_NullifierStore_test";
  private static IchApplicationContext context;
  private static BytesCapsule nullifier1;
  private static BytesCapsule nullifier2;
  private static BytesCapsule nullifier2New;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new IchApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    nullifierStore = context.getBean(NullifierStore.class);
    nullifier1 = new BytesCapsule(NULLIFIER_ONE);
    nullifier2 = new BytesCapsule(TRX_TWO);
    nullifier2New = new BytesCapsule(TRX_TWO_NEW);

    nullifierStore.put(nullifier1);
    nullifierStore.put(NULLIFIER_TWO, nullifier2);
  }

  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Wallet.getAddressPreFixByte();
    return result;
  }

  @Test
  public void putAndGet() {
    byte[] nullifier = nullifierStore.get(NULLIFIER_ONE).getData();
    Assert.assertArrayEquals("putAndGet1", nullifier, NULLIFIER_ONE);

    nullifier = nullifierStore.get(NULLIFIER_TWO).getData();
    Assert.assertArrayEquals("putAndGet2", nullifier, TRX_TWO);

    nullifierStore.put(NULLIFIER_TWO, nullifier2New);
    nullifier = nullifierStore.get(NULLIFIER_TWO).getData();
    Assert.assertArrayEquals("putAndGet2New", nullifier, TRX_TWO_NEW);
  }

  @Test
  public void putAndHas() {
    Boolean result = nullifierStore.has(NULLIFIER_ONE);
    Assert.assertTrue("putAndGet1", result);
    result = nullifierStore.has(NULLIFIER_TWO);
    Assert.assertTrue("putAndGet2", result);
  }
}