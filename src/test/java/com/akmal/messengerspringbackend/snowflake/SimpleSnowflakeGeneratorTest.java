package com.akmal.messengerspringbackend.snowflake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 20:00
 * @project messenger-spring-backend
 * @since 1.0
 */
class SimpleSnowflakeGeneratorTest {

  private static final Instant CUSTOM_EPOCH = Instant.parse("2000-06-01T00:00:00Z");
  private static final int CUSTOM_NODE_ID = 678;
  public static final int DELTA_MS = 1000;

  SimpleSnowflakeGenerator snowflakeGenerator;


  @Test
  @DisplayName("Should create an instance of the generator with default epoch (1970) and generate node ID")
  void shouldCreateInstanceWithDefaultEpochAndNodeId() {
    final var generator = SimpleSnowflakeGenerator.defaultInstance();

    assertThat(generator.epochMilli())
        .isEqualTo(Instant.EPOCH.toEpochMilli());

    this.assertNodeId(generator);
  }

  @Test
  @DisplayName("Should create an instance of the generator with custom epoch and generate node ID")
  void shouldCreateInstanceWithCustomEpoch() {
    final var generator = SimpleSnowflakeGenerator.withCustomEpoch(CUSTOM_EPOCH.toEpochMilli());

    assertThat(generator.epochMilli())
        .isEqualTo(CUSTOM_EPOCH.toEpochMilli());

    this.assertNodeId(generator);
  }

  private void assertNodeId(SnowflakeGenerator snowflakeGenerator) {
    assertThat(snowflakeGenerator.nodeId())
        .isBetween(0, ((int) Math.pow(2, 10)));
  }

  @Test
  @DisplayName("Should create the generator with custom node id and default Instant.EPOCH")
  void shouldCreateWithNodeId() {
    final var generator = SimpleSnowflakeGenerator.withNodeId(CUSTOM_NODE_ID);

    assertThat(generator.nodeId())
        .isEqualTo(CUSTOM_NODE_ID);

    assertThat(generator.epochMilli())
        .isEqualTo(Instant.EPOCH.toEpochMilli());
  }

  @Test
  @DisplayName("Should failt to create an instance with a node id greater than 1023 and smaller than 0")
  void shouldFailWithInvalidNodeId() {

    assertThatThrownBy(() -> {
      SimpleSnowflakeGenerator.withNodeId(1024);
    }, "Exception was not thrown even though the node ID exceeds (2^10 - 1)");

    assertThatThrownBy(() -> {
      SimpleSnowflakeGenerator.withNodeId(-1);
    }, "Exception was not thrown even though the node ID is less than 0");
  }

  @Test
  @DisplayName("Should create an instance of the generator with custom epoch and node id")
  void shouldCreateWithCustomEpochAndNodeId() {
    final var generator = SimpleSnowflakeGenerator.withCustomEpochAndNodeId(CUSTOM_EPOCH.toEpochMilli(),
        CUSTOM_NODE_ID);

    assertThat(generator.nodeId())
        .isEqualTo(CUSTOM_NODE_ID);

    assertThat(generator.epochMilli())
        .isEqualTo(CUSTOM_EPOCH.toEpochMilli());
  }

  @Test
  void shouldGenerateIncreasingIds() {
    final var generator = SimpleSnowflakeGenerator.defaultInstance();

    long prevSnowflake = generator.nextId();

    for (int i = 0; i < 1000; i++) {
      long snowflake = generator.nextId();

      assertThat(prevSnowflake)
          .isLessThan(snowflake);
      prevSnowflake = snowflake;
    }
  }

  @Test
  @DisplayName("Assert that 1 bit is reserved and is always 0 (MSB), 41 bits are for the timestamp, "
                   + "10 bits for node id and 12 bits for sequence numbers")
  void assertConsistencyOfSnowflakeStructure() {
    final var generator = SimpleSnowflakeGenerator.defaultInstance();

    final var snowflake = generator.nextId();

    final var extraBit = snowflake >> 63;
    final var timestamp = snowflake >> 22;
    final var nodeId =  (((1 << 22) - 1) & snowflake) >> 12;

    assertThat(extraBit).isEqualTo(0);
    assertThat(timestamp).isBetween(Instant.now().minusMillis(DELTA_MS).toEpochMilli(),
        Instant.now().plusMillis(DELTA_MS).toEpochMilli());
    assertThat(nodeId).isEqualTo(generator.nodeId());
  }

  @Test
  @DisplayName("Should extract timestamp using bitwise operations from a snowflake")
  void shouldExtractTimestampFromSnowflake() {
    final var generator = SimpleSnowflakeGenerator.defaultInstance();

    final var timestamp = generator.extractTimestamp(generator.nextId());

    assertThat(timestamp).isBetween(Instant.now().minusMillis(DELTA_MS).toEpochMilli(),
        Instant.now().plusMillis(DELTA_MS).toEpochMilli());
  }

  @Test
  @DisplayName("Should construct correct instant from a snowflake")
  void shouldGiveCorrectInstant() {
    final var generator = SimpleSnowflakeGenerator.defaultInstance();

    final var timestampInstant = generator.toInstant(generator.nextId());

    assertThat(timestampInstant).isNotNull();
    assertThat(timestampInstant).isBetween(Instant.now().minusMillis(DELTA_MS),
        Instant.now().plusMillis(DELTA_MS));
  }
}
