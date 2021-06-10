package org.ich.core.exception;

public class IchRuntimeException extends RuntimeException {

  public IchRuntimeException() {
    super();
  }

  public IchRuntimeException(String message) {
    super(message);
  }

  public IchRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public IchRuntimeException(Throwable cause) {
    super(cause);
  }

  protected IchRuntimeException(String message, Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
