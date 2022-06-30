package com.akmal.messengerspringbackend.snowflake.exception;

/**
 * Class represents the custom {@link RuntimeException} exception that is thrown in case the
 * initialization of the generator fails.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 17:59
 * @project messenger-spring-backend
 * @since 1.0
 */
public class SnowflakeGeneratorInitializationException extends RuntimeException {

  public SnowflakeGeneratorInitializationException() {
    super("Error occurred while initializing the SnowflakeGenerator");
  }

  public SnowflakeGeneratorInitializationException(String reason) {
    super(
        String.format(
            "Error occurred while initializing the SnowflakeGenerator. Reason: %s", reason));
  }

  public SnowflakeGeneratorInitializationException(String reason, Throwable cause) {
    super(
        String.format(
            "Error occurred while initializing the SnowflakeGenerator. Reason: %s", reason),
        cause);
  }

  public SnowflakeGeneratorInitializationException(Throwable cause) {
    super("Error occurred while initializing the SnowflakeGenerator", cause);
  }

  public SnowflakeGeneratorInitializationException(
      String reason, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(
        String.format(
            "Error occurred while initializing the SnowflakeGenerator. Reason: %s", reason),
        cause,
        enableSuppression,
        writableStackTrace);
  }
}
