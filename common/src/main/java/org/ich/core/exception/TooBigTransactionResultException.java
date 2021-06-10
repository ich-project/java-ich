package org.ich.core.exception;

public class TooBigTransactionResultException extends IchException {

  public TooBigTransactionResultException() {
    super("too big transaction result");
  }

  public TooBigTransactionResultException(String message) {
    super(message);
  }
}
