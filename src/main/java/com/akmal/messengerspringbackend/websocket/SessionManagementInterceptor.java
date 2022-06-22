package com.akmal.messengerspringbackend.websocket;

import com.akmal.messengerspringbackend.websocket.storage.TopicSubscription;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSession;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 22/06/2022 - 18:54
 * @project messenger-spring-backend
 * @since 1.0
 */
@Slf4j
public class SessionManagementInterceptor implements ChannelInterceptor {

  private final WebsocketSessionStorage sessionStorage;

  private SessionManagementInterceptor(WebsocketSessionStorage sessionStorage) {
    this.sessionStorage = sessionStorage;
  }

  public static SessionManagementInterceptor withStore(WebsocketSessionStorage sessionStorage) {
    return new SessionManagementInterceptor(sessionStorage);
  }

  @Override
  public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
    final var headerAccessor = StompHeaderAccessor.wrap(message);
    final var stompCommand = headerAccessor.getCommand();
    if (stompCommand != null) {

      switch (stompCommand) {
        case CONNECT -> this.handleConnect(headerAccessor);
        case DISCONNECT -> this.handleDisconnect(headerAccessor);
        case SUBSCRIBE -> this.handleSubscribe(headerAccessor);
        case UNSUBSCRIBE -> this.handleUnsubscribe(headerAccessor);
        default -> {}
      }
    }

    return message;
  }
  private void handleUnsubscribe(StompHeaderAccessor headerAccessor) {
    final var principal = headerAccessor.getUser();
    if (principal == null) return;
    final var topicName = headerAccessor.getDestination();

    this.sessionStorage.unsubscribe(principal.getName(), topicName);
  }

  private void handleSubscribe(StompHeaderAccessor headerAccessor) {
    final var principal = headerAccessor.getUser();
    if (principal == null) return;
    final var subscriptionId = headerAccessor.getSubscriptionId();
    final var topicName = headerAccessor.getDestination();

    final var sub = TopicSubscription.builder()
                        .id(subscriptionId)
                        .joinedAt(LocalDateTime.now())
                        .topic(topicName)
                        .build();

    this.sessionStorage.addSubscription(principal.getName(), sub);
  }

  private void handleConnect(StompHeaderAccessor headerAccessor) {
    final var sessionId = headerAccessor.getSessionId();
    final var principal = headerAccessor.getUser();
    if (principal == null) return;

    InetAddress remoteAddress = null;

    if (headerAccessor.getSessionAttributes() != null &&
            headerAccessor.getSessionAttributes().containsKey("remote_address")) {
      final var socketAddress = (InetSocketAddress) headerAccessor.getSessionAttributes().get("remote_address");
      remoteAddress = socketAddress.getAddress();
    }


    final var session =
        WebsocketSession.builder()
            .joinedAt(LocalDateTime.now())
            .remoteAddress(remoteAddress)
            .subscriptions(new HashSet<>())
            .id(sessionId)
            .uid(principal.getName())
            .build();

    this.sessionStorage.add(session);
  }

  private void handleDisconnect(StompHeaderAccessor headerAccessor) {
    final var principal = headerAccessor.getUser();

    this.sessionStorage.remove(principal.getName());
  }
}
