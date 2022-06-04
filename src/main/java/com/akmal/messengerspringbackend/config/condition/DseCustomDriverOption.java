package com.akmal.messengerspringbackend.config.condition;

import com.datastax.oss.driver.api.core.config.DriverOption;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 04/06/2022 - 12:17
 * @project messenger-spring-backend
 * @since 1.0
 */
public enum DseCustomDriverOption implements DriverOption {
  MAX_READ_ATTEMPTS("advanced.retry-policy.custom-retry-policy.max-read-attempts"),
  MAX_WRITE_ATTEMPTS("advanced.retry-policy.custom-retry-policy.max-read-attempts");


  private final String path;

  DseCustomDriverOption(String path) {
    this.path = path;
  }

  @NonNull
  @Override
  public String getPath() {
    return this.path;
  }
}
