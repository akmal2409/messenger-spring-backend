package com.akmal.messengerspringbackend.websocket.storage;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 22/06/2022 - 18:14
 * @project messenger-spring-backend
 * @since 1.0
 */
@Service
@Slf4j
public class WebsocketSessionStorageImpl implements WebsocketSessionStorage {
  public final Map<String, WebsocketSession> sessions = new ConcurrentHashMap<>();

  @Override
  public void add(WebsocketSession websocketSession) {
    log.info("Adding websocket session {}", websocketSession);
    this.sessions.put(websocketSession.uid(), websocketSession);
  }

  @Override
  public boolean addSubscription(String uid, TopicSubscription sub) {
    if (!this.isUserConnected(uid)) return false;
    log.info("Adding subscription for user {} sub: {}", uid, sub);
    this.unsubscribe(uid, sub.topic());

    final var session =
        this.sessions.get(uid).toBuilder()
            .subscriptions(new HashSet<>(this.sessions.get(uid).subscriptions()))
            .build();

    session.subscriptions().add(sub);

    this.sessions.put(session.uid(), session);
    return true;
  }

  @Override
  public void remove(String uid) {
    this.sessions.remove(uid);
  }

  @Override
  public void unsubscribe(String uid, String topicName) {
    if (this.isUserConnected(uid)) return;
    final var session =
        this.sessions.get(uid).toBuilder()
            .subscriptions(new HashSet<>(this.sessions.get(uid).subscriptions()))
            .build();

    session.subscriptions().stream()
        .filter(s -> s.topic().equals(topicName))
        .findFirst()
        .ifPresent(
            s -> {
              session.subscriptions().remove(s);
              this.sessions.put(session.uid(), session);
            });
  }

  @Override
  public boolean isUserConnected(String uid) {
    return this.sessions.containsKey(uid);
  }

  @Override
  public boolean isUserSubscribedTo(String uid, String topicName) {
    if (!this.sessions.containsKey(uid)) return false;

    return this.sessions.get(uid).subscriptions().stream()
        .anyMatch(sub -> sub.topic().equals(topicName));
  }
}
