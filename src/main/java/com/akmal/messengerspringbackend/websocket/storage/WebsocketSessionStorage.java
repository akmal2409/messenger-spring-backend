package com.akmal.messengerspringbackend.websocket.storage;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 14/06/2022 - 21:21
 * @project messenger-spring-backend
 * @since 1.0
 */
public interface WebsocketSessionStorage {

  void add(WebsocketSession websocketSession);

  boolean addSubscription(String uid, TopicSubscription sub);

  void remove(String uid);

  void unsubscribe(String uid, String topicName);

  boolean isUserConnected(String uid);

  boolean isUserSubscribedTo(String uid, String topicName);
}
