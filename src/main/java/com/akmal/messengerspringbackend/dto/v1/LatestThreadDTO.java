package com.akmal.messengerspringbackend.dto.v1;

import java.util.Set;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 26/06/2022 - 17:50
 * @project messenger-spring-backend
 * @since 1.0
 */
public record LatestThreadDTO(
    String threadId,
    long lastMessageId,
    String lastMessageAt,
    String threadName,
    String threadPictureThumbnailUrl,
    String lastMessage,
    UserDetailsDTO author,
    Set<String> memberIds,
    boolean read,
    boolean systemMessage,
    boolean groupThread
) {

}
