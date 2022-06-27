package com.akmal.messengerspringbackend.shared.timeago;

import java.time.Instant;

/**
 * Contract that represents a functional interface for time to string conversions
 * in the form of "x hours ago" etc.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 18:08
 * @project messenger-spring-backend
 * @since 1.0
 */
@FunctionalInterface
public interface TimeAgoConverter {

  /**
   * Converts given Instant instance to representation that has a relative
   * period represented as a string.
   * @param time
   * @return
   */
  String convert(Instant time);
}
