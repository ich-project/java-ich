package org.ich.common.common.runtime;

import org.ich.core.db.TransactionContext;
import org.ich.core.exception.ContractExeException;
import org.ich.core.exception.ContractValidateException;


public interface Runtime {

  void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException;

  ProgramResult getResult();

  String getRuntimeError();

}
