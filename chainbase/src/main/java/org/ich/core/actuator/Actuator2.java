package org.ich.core.actuator;

import org.ich.core.exception.ContractExeException;
import org.ich.core.exception.ContractValidateException;

public interface Actuator2 {

  void execute(Object object) throws ContractExeException;

  void validate(Object object) throws ContractValidateException;
}