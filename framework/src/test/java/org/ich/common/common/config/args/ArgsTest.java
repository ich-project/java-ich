package org.ich.common.common.config.args;

import java.io.File;

import org.ich.common.common.utils.FileUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ich.core.Constant;
import org.ich.core.config.args.Args;

public class ArgsTest {

  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
  }

  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File("output-directory"));
  }

  @Test
  public void testConfig() {
    Assert.assertEquals(Args.getInstance().getMaxTransactionPendingSize(), 2000);
    Assert.assertEquals(Args.getInstance().getPendingTransactionTimeout(), 60_000);
    Assert.assertEquals(Args.getInstance().getNodeDiscoveryPingTimeout(), 15_000);
  }
}