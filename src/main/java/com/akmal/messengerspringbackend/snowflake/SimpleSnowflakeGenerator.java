package com.akmal.messengerspringbackend.snowflake;

import com.akmal.messengerspringbackend.snowflake.exception.SnowflakeGeneratorInitializationException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The following implementation will generate 64-bit unique ID and it ensures that the ID is unique
 * across different nodes. Furthermore, generated IDs are roughly time sortable. The information in
 * 64 bits is spread out following:
 *
 * <ul>
 *   <li><strong>41 bits</strong> are allocated to the milliseconds with respect to the custom epoch
 *   <li><strong>10 bits</strong> are allocated to node ID. Node ID is constructed based on the 48
 *       or 64 bit MAC address that is converted to HEX and the resulting string's hashcode is going
 *       to be the node ID itself
 *   <li><strong>12 bits are allocated to the sequence number generated and incremented
 *       locally</strong>
 *   <li><strong>Extra 1 bit</strong> is reserved
 * </ul>
 *
 * <h3>Constraints</h2>
 *
 * <p>The implementation (without taking into account an extra bit) supports roughly 69 years worth
 * of timestamps w.r.t the custom epoch. Furthermore, 10 bits that are allocated for the node ID,
 * which lets us have 1024 nodes. Lastly, 12 bits for the seq. no allow us to have a counter of 4096
 * values, which rolls over once overflown.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 17:20
 * @project messenger-spring-backend
 * @since 1.0
 */
public class SimpleSnowflakeGenerator implements SnowflakeGenerator {
  private static final Logger log = LoggerFactory.getLogger(SimpleSnowflakeGenerator.class);
  private static final int NUMBER_OF_BITS_TIMESTAMP = 41;
  private static final int NUMBER_OF_BITS_NODE_ID = 10;
  private static final int NUMBER_OF_BITS_SEQ_NO = 12;
  private static final int NUMBER_OF_EXTRA_BITS = 1;

  private static final int SHIFT_BY_FOR_TIMESTAMP = NUMBER_OF_BITS_SEQ_NO + NUMBER_OF_BITS_NODE_ID;

  private static final int MAX_NODE_ID = (int) Math.pow(2, NUMBER_OF_BITS_NODE_ID) - 1;
  private static final int MAX_SEQ_NO = (int) Math.pow(2, NUMBER_OF_BITS_SEQ_NO) - 1;
  private static final long MAX_PERIOD_MS = (long) Math.pow(2, NUMBER_OF_BITS_TIMESTAMP);

  private final long customEpoch;
  private final int nodeId;

  private final AtomicInteger sequenceNumber;

  private volatile long lastTimestamp = this.timestamp() - 100;

  private SimpleSnowflakeGenerator(long customEpoch, int nodeId) {
    if (nodeId < 0 || nodeId > MAX_NODE_ID) {
      throw new SnowflakeGeneratorInitializationException(
          "Node ID either is negative or exceeds the maximum value of "
              .concat(String.valueOf(MAX_NODE_ID)));
    }

    if (customEpoch < 0)
      throw new SnowflakeGeneratorInitializationException("Custom epoch cannot be negative");

    this.customEpoch = customEpoch;
    this.nodeId = nodeId;
    this.sequenceNumber = new AtomicInteger(new SecureRandom().nextInt(MAX_SEQ_NO));
  }

  private SimpleSnowflakeGenerator(long customEpoch) {
    if (customEpoch < 0)
      throw new SnowflakeGeneratorInitializationException("Custom epoch cannot be negative");

    this.customEpoch = customEpoch;
    this.nodeId = this.generateNodeId();
    this.sequenceNumber = new AtomicInteger(new SecureRandom().nextInt(MAX_SEQ_NO));
  }

  private SimpleSnowflakeGenerator() {
    this(Instant.EPOCH.toEpochMilli());
  }

  /**
   * Static factory that returns an instance {@link SnowflakeGenerator} with default epoch set to
   * the one during instantiation and generated node id.
   *
   * @return {@link SimpleSnowflakeGenerator} instance
   */
  public static SnowflakeGenerator defaultInstance() {
    return new SimpleSnowflakeGenerator();
  }

  /**
   * Static factory that creates an instance of {@link SnowflakeGenerator} with custom epoch in
   * milliseconds.
   *
   * @param epochMilli custom epoch in milliseconds
   * @return {@link SimpleSnowflakeGenerator} instance
   */
  public static SnowflakeGenerator withCustomEpoch(long epochMilli) {
    return new SimpleSnowflakeGenerator(epochMilli);
  }

  /**
   * Static factory that creates an instance of {@link SnowflakeGenerator} with custom nodeId.
   *
   * @param nodeId unique machine identifier
   * @return {@link SimpleSnowflakeGenerator} instance
   */
  public static SnowflakeGenerator withNodeId(int nodeId) {
    return new SimpleSnowflakeGenerator(Instant.EPOCH.toEpochMilli(), nodeId);
  }

  /**
   * Static factory that creates an instance of {@link SnowflakeGenerator} with custom node id and
   * epoch (in milliseconds)
   *
   * @param epochMilli custom epoch in milliseconds
   * @param nodeId custom node identifier (unique)
   * @return {@link SimpleSnowflakeGenerator} instance
   */
  public static SnowflakeGenerator withCustomEpochAndNodeId(long epochMilli, int nodeId) {
    return new SimpleSnowflakeGenerator(epochMilli, nodeId);
  }

  /**
   * Snowflake of 64 bits has the next format:
   *
   * <p>[1 bit (always 0)] [41 bits timestamp] [10 bits machine id] [12 bits seq no]
   *
   * @return snowflake distributed id.
   */
  @Override
  public synchronized long nextId() {
    long snowflake = 0;

    long currentTimestamp = this.timestamp();

    int seqNo = this.sequenceNumber.getAndUpdate(value -> (value + 1) % (MAX_SEQ_NO + 1));

    if (currentTimestamp == this.lastTimestamp) {
      if (seqNo == 0) {
        // block until we ensure that 1ms passes because sequence is exhausted
        while (currentTimestamp == this.lastTimestamp) {
          currentTimestamp = this.timestamp();
        }
      }
    }

    this.lastTimestamp = currentTimestamp;

    snowflake |= seqNo; // set first 12 bits

    snowflake |= ((long) this.nodeId << NUMBER_OF_BITS_SEQ_NO);

    snowflake |= (currentTimestamp << NUMBER_OF_BITS_SEQ_NO + NUMBER_OF_BITS_NODE_ID);

    return snowflake;
  }

  /**
   * Timestamp begins at the 22nd bit, therefore we have to shift it by 22 places.
   *
   * @param id - snowflake id.
   * @return timestamp in milliseconds since the epoch
   */
  @Override
  public long toTimestampMilli(long id) {
    return id >> SHIFT_BY_FOR_TIMESTAMP;
  }

  @Override
  public Instant toInstant(long id) {
    return Instant.ofEpochMilli(this.customEpoch).plusMillis(this.toTimestampMilli(id));
  }

  @Override
  public long epochMilli() {
    return this.customEpoch;
  }

  @Override
  public int nodeId() {
    return this.nodeId;
  }

  private long timestamp() {
    return Instant.now().toEpochMilli() - this.customEpoch;
  }

  /**
   * The method generates a unique node ID by means of hashing the hardware address of a non-virtual
   * network interface. For the initialization to succeed there has to be at least
   * <strong>1</strong> non-virtual network interface that has a hardware address. Furthermore, it
   * reads the MAC address (which can be 48 or 64 bits) as a sequence of bytes and converts it into
   * a string, on which we then call {@link String#hashCode()} to get an integer representation and
   * transform the resulting hashcode to a node id by performing bitwise AND with {@link
   * SimpleSnowflakeGenerator#MAX_NODE_ID}. W use bitwise AND to ensure that all the MSBs that
   * exceed MSBs of MAX_NODE_ID are set to 0 and the number stays within the bound.
   *
   * @throws SnowflakeGeneratorInitializationException if the generation of the ID fails.
   * @return unique node id within range {0, {@link SimpleSnowflakeGenerator#MAX_NODE_ID}}
   *     inclusive.
   */
  private int generateNodeId() {
    try {
      final Enumeration<NetworkInterface> networkInterfaceIterator =
          NetworkInterface.getNetworkInterfaces();

      while (networkInterfaceIterator.hasMoreElements()) {
        final var networkInterface = networkInterfaceIterator.nextElement();

        final byte[] mac = networkInterface.getHardwareAddress();

        if (mac != null) {
          // filtering out virtual interfaces. All virtual interfaces do not have hardware address.
          final var sb = new StringBuilder();

          for (byte b : mac) {
            sb.append(String.format("%02x", b));
          }

          final var humanReadableMac =
              String.join("-", sb.toString().split("(?<=\\G.{2})")).toUpperCase();

          log.info(
              "Using network interface for node ID generation, with name '{}' and hardware address '{}'",
              networkInterface.getDisplayName(),
              humanReadableMac);

          // making sure that the node ID does not exceed the max value.
          final var nodeId = sb.toString().hashCode() & MAX_NODE_ID;

          log.info("Generated unique node ID {}", nodeId);
          return nodeId;
        }
      }

      throw new SnowflakeGeneratorInitializationException(
          "Could not generate node ID. " + "Cause: No hardware address found");
    } catch (SocketException cause) {
      throw new SnowflakeGeneratorInitializationException(
          "Unable to read network interfaces. Make sure that there is at least 1 network interface.",
          cause);
    }
  }
}
