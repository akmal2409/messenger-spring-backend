package com.akmal.messengerspringbackend.shared.timeago;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class implements {@link TimeAgoConverter} and converts given point in time to a string
 * representation.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 18:09
 * @project messenger-spring-backend
 * @since 1.0
 */
class SimpleTimeAgoConverter implements TimeAgoConverter {
  private static final String DATE_FORMAT_PATTERN = "dd/MM/yyyy";

  private final DateTimeFormatter formatter;

  SimpleTimeAgoConverter() {
    this.formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
  }

  SimpleTimeAgoConverter(@NotNull DateTimeFormatter formatter) {
    this.formatter = formatter;
  }

  /**
   * The method converts the given time instant relative to the current system time. <strong>The
   * following results are to be expected when given the following input</strong> <br>
   *
   * <p>Let <strong>now()</strong> be the function that represents the current system time
   *
   * <p>Let <strong>time</strong> be the argument passed to the function
   *
   * <ul>
   *   <li>(now() - time) <= 60 -> 'Now'
   *   <li>(now() - time) <= 120 -> 'A minute ago'
   *   <li>(now() - time) <= 3600 -> '{round(floor( (now() - time)/60 ))} minutes ago'
   *   <li>(now() - time) <= 3600 * 2 -> 'An hour ago'
   *   <li>(now() - time) <= 3600 * 24 -> '{round(floor( (now() - time)/3600 ))} hours ago'
   *   <li>(now() - time) <= 3600 * 24 * 2 -> 'A day ago'
   *   <li>(now() - time) <= 3600 * 24 * 7 -> '{round(floor( (now() - time)/3600 * 24 ))} days ago'
   * </ul>
   *
   * @throws IllegalArgumentException if the time is past the current system time.
   * @param time {@link Instant} non null point in time
   * @return time in a string representation.
   */
  @Contract(pure = true)
  @Override
  public String convert(@NotNull Instant time) {
    final var currentTime = Instant.now();

    if (currentTime.isBefore(time)) {
      throw new IllegalArgumentException(
          String.format(
              "Expected time before now '%s'. Got '%s' which is in the future.",
              currentTime.toEpochMilli(), time.toEpochMilli()));
    }

    final long difference = currentTime.getEpochSecond() - time.getEpochSecond();

    if (difference < 60) return "Now";
    else if (difference < 120) return "A minute";
    else if (difference < 3600)
      return String.format("%d minutes", Math.round(Math.floor(difference / (double) 60)));
    else if (difference < 3600 * 2) return "An hour";
    else if (difference < 3600 * 24)
      return String.format("%d hours", Math.round(Math.floor(difference / (double) 3600)));
    else if (difference < 3600 * 24 * 2) return "A day";
    else if (difference < 3600 * 24 * 7)
      return String.format(
          "%d days", Math.round(Math.floor(difference / ((double) 3600 * 24))));
    else return time.atZone(ZoneId.systemDefault()).format(this.formatter);
  }
}
