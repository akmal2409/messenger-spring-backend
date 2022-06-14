package com.akmal.messengerspringbackend.shared.datastructure;

import lombok.Builder;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 12/06/2022 - 15:28
 * @project messenger-spring-backend
 * @since 1.0
 */
@Builder
public record Tuple<E, V>(
    E e1,
    V e2
) {
}
