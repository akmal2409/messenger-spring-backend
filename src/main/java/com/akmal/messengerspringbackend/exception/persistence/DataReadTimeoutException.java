package com.akmal.messengerspringbackend.exception.persistence;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/06/2022 - 19:56
 * @project messenger-spring-backend
 * @since 1.0
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DataReadTimeoutException extends DataAccessException {

  public DataReadTimeoutException(String msg) {
    super(msg);
  }

  public DataReadTimeoutException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
