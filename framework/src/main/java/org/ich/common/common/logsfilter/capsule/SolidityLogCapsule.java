package org.ich.common.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.ich.common.common.logsfilter.EventPluginLoader;
import org.ich.common.common.logsfilter.trigger.ContractLogTrigger;

public class SolidityLogCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private ContractLogTrigger solidityLogTrigger;

  public SolidityLogCapsule(ContractLogTrigger solidityLogTrigger) {
    this.solidityLogTrigger = solidityLogTrigger;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postSolidityLogTrigger(solidityLogTrigger);
  }
}