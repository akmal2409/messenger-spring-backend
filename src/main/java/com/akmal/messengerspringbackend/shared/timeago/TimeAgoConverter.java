package com.akmal.messengerspringbackend.shared.timeago;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.jetbrains.annotations.NotNull;

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

  /**
   * Returns an instance of a {@link TimeAgoConverter} with a custom {@link DateTimeFormatter}.
   * Formatter is being used when the time is larger than a week, in that case the converter
   * spits out the full date with accordance to the format specified in {@link DateTimeFormatter}.
   *
   * @param formatter non null
   * @return {@link TimeAgoConverter} instance with a custom formatter.
   */
  static TimeAgoConverter withDateFormatter(@NotNull DateTimeFormatter formatter) {
    return new SimpleTimeAgoConverter(formatter);
  }

  /**
   * Returns an instance of a {@link TimeAgoConverter} with a default {@link DateTimeFormatter} of a
   * following pattern 'dd/MM/yyyy'. The converter is invoked if the time passed is larger than 1 week.
   *
   * @return {@link TimeAgoConverter} instance with a default formatter.
   */
  static SimpleTimeAgoConverter withDefaults() {
    return new SimpleTimeAgoConverter();
  }
}
