package com.akmal.messengerspringbackend.dto.v1;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 12/07/2022 - 18:46
 * @project messenger-spring-backend
 * @since 1.0
 */
public record MessageAcknowledgement(
    MessageDTO message,
    String receiptId,
    boolean success
) {

}
