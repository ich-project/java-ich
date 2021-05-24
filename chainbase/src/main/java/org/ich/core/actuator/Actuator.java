package org.ich.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.ich.core.exception.ContractExeException;
import org.ich.core.exception.ContractValidateException;

public interface Actuator {

  boolean execute(Object result) throws ContractExeException;

  boolean validate() throws ContractValidateException;

  ByteString getOwnerAddress() throws InvalidProtocolBufferException;

  long calcFee();

}
