package org.ich.common.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.ich.common.common.application.TronApplicationContext;
import org.ich.common.common.storage.Deposit;
import org.ich.common.common.storage.DepositImpl;
import org.ich.common.common.utils.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.spongycastle.util.encoders.Hex;
import org.ich.common.common.runtime.Runtime;
import org.ich.core.Constant;
import org.ich.core.Wallet;
import org.ich.core.config.DefaultConfig;
import org.ich.core.config.args.Args;
import org.ich.core.db.Manager;
import org.ich.protos.Protocol.AccountType;

@Slf4j
public class VMTestBase {

  protected Manager manager;
  protected TronApplicationContext context;
  protected String dbPath;
  protected Deposit rootDeposit;
  protected String OWNER_ADDRESS;
  protected Runtime runtime;

  @Before
  public void init() {
    dbPath = "output_" + this.getClass().getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    manager = context.getBean(Manager.class);
    rootDeposit = DepositImpl.createRoot(manager);
    rootDeposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    rootDeposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);

    rootDeposit.commit();
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
  }

}
