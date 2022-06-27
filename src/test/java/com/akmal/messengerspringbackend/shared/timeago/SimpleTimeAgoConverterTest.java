package com.akmal.messengerspringbackend.shared.timeago;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 18:39
 * @project messenger-spring-backend
 * @since 1.0
 */
class SimpleTimeAgoConverterTest {

  TimeAgoConverter simpleTimeAgoConverter;
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @BeforeEach
  void setup() {
    this.simpleTimeAgoConverter = SimpleTimeAgoConverter.withDateFormatter(this.formatter);
  }

  @Test
  @DisplayName("Should fail when input time is null")
  void testInputTimeNull() {
    assertThatThrownBy(() -> {
      this.simpleTimeAgoConverter.convert(null);
    }).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Should fail when input time is in the future")
  void testInputTimeInTheFuture() {
    final var time = Instant.now().plusMillis(1);

    assertThatThrownBy(() -> {
      this.simpleTimeAgoConverter.convert(time);
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should return 'Now' when given time with difference < 60 seconds")
  void testShowsNow() {
    final var time = Instant.now().minus(Duration.ofSeconds(59));
    this.testExpectedTimeAndString(time, "Now");

  }

  @Test
  @DisplayName("Should return 'A minute ago' when given time with 60 <= difference < 120 ")
  void testShowsMinuteAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(60));
    this.testExpectedTimeAndString(time, "A minute ago");
  }

  @Test
  @DisplayName("Should return '2 minutes ago' when given time with 120 <= difference < 180  ")
  void testShows2MinAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(120));
    this.testExpectedTimeAndString(time, "2 minutes ago");
  }

  @Test
  @DisplayName("Should return '59 minutes ago' when given time with 59 * 60 <= difference < 60 * 60  ")
  void testShows59MinAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(59 * 60));
    this.testExpectedTimeAndString(time, "59 minutes ago");
  }

  @Test
  @DisplayName("Should return 'An hour ago' when given time with 60 * 60 <= difference < 2 * 60 * 60  ")
  void testShows1HAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(3600));
    this.testExpectedTimeAndString(time, "An hour ago");
  }

  @Test
  @DisplayName("Should return '2 hours ago' when given time with 2 * 60 * 60 <= difference < 3 * 60 * 60  ")
  void testShows2HAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(2 * 3600));
    this.testExpectedTimeAndString(time, "2 hours ago");
  }

  @Test
  @DisplayName("Should return '23 hours ago' when given time with 23 * 60 * 60 <= difference < 24 * 60 * 60  ")
  void testShows23HAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(23 * 3600));
    this.testExpectedTimeAndString(time, "23 hours ago");
  }

  @Test
  @DisplayName("Should return 'A day ago' when given time with 24 * 60 * 60 <= difference < 2 * 24 * 60 * 60  ")
  void testShows1DAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(24 * 3600));
    this.testExpectedTimeAndString(time, "A day ago");
  }

  @Test
  @DisplayName("Should return '2 days ago' when given time with 2 * 24 * 60 * 60 <= difference < 3 * 24 * 60 * 60  ")
  void testShows2DAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(2 * 24 * 3600));
    this.testExpectedTimeAndString(time, "2 days ago");
  }

  @Test
  @DisplayName("Should return '6 days ago' when given time with 6 * 24 * 60 * 60 <= difference < 7 * 24 * 60 * 60  ")
  void testShows6DAgo() {
    final var time = Instant.now().minus(Duration.ofSeconds(6 * 24 * 3600));
    this.testExpectedTimeAndString(time, "6 days ago");
  }

  @Test
  @DisplayName("Should return 'full date' when given time with 7 * 24 * 60 * 60 <= difference")
  void testShowsFullDate() {
    final var expectedTimeString = Instant.EPOCH.atZone(ZoneId.systemDefault())
                                       .format(this.formatter);

    final var time = Instant.EPOCH;
    this.testExpectedTimeAndString(time, expectedTimeString);
  }

  void testExpectedTimeAndString(Instant time, String expected) {
    final var result = this.simpleTimeAgoConverter.convert(time);
    assertThat(result).isNotNull()
        .isEqualTo(expected);
  }
}
