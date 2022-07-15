package com.akmal.messengerspringbackend.dto.v1;

import com.akmal.messengerspringbackend.user.UserPresenceEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 14/07/2022 - 18:41
 * @project messenger-spring-backend
 * @since 1.0
 */
public record UserPresenceEventDTO(
    String uid,
    Instant lastSeenAt
) {

  public static UserPresenceEventDTO fromUserPresenceEvent(UserPresenceEvent presenceEvent) {
    return new UserPresenceEventDTO(
        presenceEvent.getUserId().toString(),
        Instant.ofEpochMilli(presenceEvent.getLastSeenAt())
    );
  }
}
