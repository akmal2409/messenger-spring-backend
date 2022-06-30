package com.akmal.messengerspringbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The exception is thrown in case there are violations during the thread creation between the
 * users.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 27/06/2022 - 20:29
 * @project messenger-spring-backend
 * @since 1.0
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IllegalThreadCreationRequest extends RuntimeException {

  public IllegalThreadCreationRequest(String message) {
    super(message);
  }

  public IllegalThreadCreationRequest(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalThreadCreationRequest(Throwable cause) {
    super(cause);
  }

  public IllegalThreadCreationRequest(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
