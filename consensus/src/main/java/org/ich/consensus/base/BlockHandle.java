package org.ich.consensus.base;

import org.ich.consensus.base.Param.Miner;
import org.ich.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Miner miner, long blockTime, long timeout);

}