package com.akmal.messengerspringbackend.dto.v1;

import java.util.Collection;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 03/06/2022 - 19:30
 * @project messenger-spring-backend
 * @since 1.0
 */
public record Page<T>(
    Collection<T> content,
    int page,
    int size,
    boolean hasNext
) {

}
