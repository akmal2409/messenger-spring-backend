package com.akmal.messengerspringbackend.repository;

import com.akmal.messengerspringbackend.model.Thread;
import com.akmal.messengerspringbackend.model.ThreadByUserByLastMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 02/06/2022 - 18:38
 * @project messenger-spring-backend
 * @since 1.0
 */
public interface ThreadRepository {

  Optional<Thread> findByThreadId(UUID threadId);

  List<ThreadByUserByLastMessage> findThreadByLastMessageByUser(UUID uid);
}