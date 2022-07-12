package com.akmal.messengerspringbackend.websocket.storage;

import java.time.Instant;
import lombok.Builder;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 22/06/2022 - 18:05
 * @project messenger-spring-backend
 * @since 1.0
 */
@Builder
public record TopicSubscription(
  String topic,
  Instant joinedAt,
  String id
) {

}
