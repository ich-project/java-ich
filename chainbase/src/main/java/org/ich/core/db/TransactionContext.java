package org.ich.core.db;

import lombok.Data;
import org.ich.common.common.runtime.ProgramResult;
import org.ich.core.capsule.BlockCapsule;
import org.ich.core.capsule.TransactionCapsule;
import org.ich.core.store.StoreFactory;

@Data
public class TransactionContext {

  private BlockCapsule blockCap;
  private TransactionCapsule trxCap;
  private StoreFactory storeFactory;
  private ProgramResult programResult = new ProgramResult();
  private boolean isStatic;
  private boolean eventPluginLoaded;

  public TransactionContext(BlockCapsule blockCap, TransactionCapsule trxCap,
      StoreFactory storeFactory,
      boolean isStatic,
      boolean eventPluginLoaded) {
    this.blockCap = blockCap;
    this.trxCap = trxCap;
    this.storeFactory = storeFactory;
    this.isStatic = isStatic;
    this.eventPluginLoaded = eventPluginLoaded;
  }
}
