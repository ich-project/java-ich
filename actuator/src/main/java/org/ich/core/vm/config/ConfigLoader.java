package org.ich.core.vm.config;


import static org.ich.core.capsule.ReceiptCapsule.checkForEnergyLimit;

import lombok.extern.slf4j.Slf4j;
import org.ich.common.common.parameter.CommonParameter;
import org.ich.core.store.DynamicPropertiesStore;
import org.ich.core.store.StoreFactory;

@Slf4j(topic = "VMConfigLoader")
public class ConfigLoader {

  //only for unit test
  public static boolean disable = false;

  public static void load(StoreFactory storeFactory) {
    if (!disable) {
      DynamicPropertiesStore ds = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
      VMConfig.setVmTrace(CommonParameter.getInstance().isVmTrace());
      if (ds != null) {
        VMConfig.initVmHardFork(checkForEnergyLimit(ds));
        VMConfig.initAllowMultiSign(ds.getAllowMultiSign());
        VMConfig.initAllowTvmTransferTrc10(ds.getAllowTvmTransferTrc10());
        VMConfig.initAllowTvmConstantinople(ds.getAllowTvmConstantinople());
        VMConfig.initAllowTvmSolidity059(ds.getAllowTvmSolidity059());
        VMConfig.initAllowShieldedTRC20Transaction(ds.getAllowShieldedTRC20Transaction());
        VMConfig.initAllowTvmIstanbul(ds.getAllowTvmIstanbul());
        VMConfig.initAllowTvmStake(ds.getAllowTvmStake());
        VMConfig.initAllowTvmAssetIssue(ds.getAllowTvmAssetIssue());
        VMConfig.initAllowTvmFreeze(ds.getAllowTvmFreeze());
      }
    }
  }
}
