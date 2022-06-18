package com.akmal.messengerspringbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 18:49
 * @project messenger-spring-backend
 * @since 1.0
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedActionException extends RuntimeException {

  public UnauthorizedActionException(String message) {
    super(message);
  }

  public UnauthorizedActionException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnauthorizedActionException(Throwable cause) {
    super(cause);
  }

  public UnauthorizedActionException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
