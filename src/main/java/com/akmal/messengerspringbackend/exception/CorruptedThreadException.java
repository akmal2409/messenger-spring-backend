package com.akmal.messengerspringbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 02/06/2022 - 18:28
 * @project messenger-spring-backend
 * @since 1.0
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CorruptedThreadException extends RuntimeException {

  public CorruptedThreadException(String message) {
    super(message);
  }

  public CorruptedThreadException(String message, Throwable cause) {
    super(message, cause);
  }

  public CorruptedThreadException(Throwable cause) {
    super(cause);
  }

  public CorruptedThreadException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
