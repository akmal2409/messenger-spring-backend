package com.akmal.messengerspringbackend.websocket.storage;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 19:58
 * @project messenger-spring-backend
 * @since 1.0
 */
@Builder(toBuilder = true)
public record WebsocketSession(
    InetAddress remoteAddress,
    String uid,
    Instant joinedAt,
    Set<TopicSubscription> subscriptions,
    String id
) {

}
