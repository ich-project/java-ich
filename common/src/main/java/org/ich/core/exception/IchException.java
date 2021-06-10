package org.ich.core.exception;

public class IchException extends Exception {

  public IchException() {
    super();
  }

  public IchException(String message) {
    super(message);
  }

  public IchException(String message, Throwable cause) {
    super(message, cause);
  }

}
