package com.akmal.messengerspringbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 17/06/2022 - 19:41
 * @project messenger-spring-backend
 * @since 1.0
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthorizationException extends RuntimeException {
  public AuthorizationException(String message) {
    super(message);
  }

  public AuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }

  public AuthorizationException(Throwable cause) {
    super(cause);
  }

  public AuthorizationException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
