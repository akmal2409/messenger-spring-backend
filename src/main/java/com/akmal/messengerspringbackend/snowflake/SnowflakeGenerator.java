package com.akmal.messengerspringbackend.snowflake;

import java.time.Instant;

/**
 * The interface represents the common contract for the distributed ID generation.
 * Generally speaking, any implementation must ensure that the generated IDs are unique across
 * different nodes and do not collide, as well as being roughly time sortable.
 * All implementation will fit the entire ID in 64 bits. The arrangement of data inside
 * depends on the implementation class.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 01/06/2022 - 17:16
 * @project messenger-spring-backend
 * @since 1.0
 */
public interface SnowflakeGenerator {

  long nextId();

  long extractTimestamp(long id);

  long epochMilli();

  int nodeId();

  /**
   * The method converts number of seconds that was created with respect to
   * custom epoch to UNIX epoch.
   *
   * @return number of milliseconds from {@link java.time.Instant#EPOCH}
   */
  Instant toInstant(long id);
}
