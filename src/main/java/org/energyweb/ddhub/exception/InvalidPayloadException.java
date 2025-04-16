package org.energyweb.ddhub.exception;

public class InvalidPayloadException extends RuntimeException {
  public InvalidPayloadException(String message) {
    super(message);
  }

  public InvalidPayloadException(String message, Throwable cause) {
    super(message, cause);
  }
}
