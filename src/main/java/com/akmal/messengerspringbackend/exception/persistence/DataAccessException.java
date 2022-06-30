package com.akmal.messengerspringbackend.exception.persistence;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 28/06/2022 - 20:00
 * @project messenger-spring-backend
 * @since 1.0
 */
public class DataAccessException extends org.springframework.dao.DataAccessException {

  public DataAccessException(String msg) {
    super(msg);
  }

  public DataAccessException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
