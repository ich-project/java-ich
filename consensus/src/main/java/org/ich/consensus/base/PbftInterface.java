package org.ich.consensus.base;

import org.ich.consensus.pbft.message.PbftBaseMessage;
import org.ich.core.capsule.BlockCapsule;

public interface PbftInterface {

  boolean isSyncing();

  void forwardMessage(PbftBaseMessage message);

  BlockCapsule getBlock(long blockNum) throws Exception;

}