package com.akmal.messengerspringbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 20:57
 * @project messenger-spring-backend
 * @since 1.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {

  public EntityNotFoundException() {
    super("Entity with given ID was not found");
  }

  public EntityNotFoundException(String message) {
    super(message);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public EntityNotFoundException(Throwable cause) {
    super(cause);
  }

  public EntityNotFoundException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
