package com.akmal.messengerspringbackend.config.websocket;

import com.akmal.messengerspringbackend.websocket.BearerHandshakeInterceptor;
import com.akmal.messengerspringbackend.websocket.IpHandshakeInterceptor;
import com.akmal.messengerspringbackend.websocket.SessionManagementInterceptor;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 14/06/2022 - 19:11
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
  public static final String NOTIFICATION_TOPIC = "/user/queue/notifications";
  public static final String ERROR_TOPIC = "/user/queue/errors";
  public static final String THREAD_TOPIC = "/user/queue/threads"; // + threadId TODO: secure threads by checking subscriber

  private final JwtDecoder jwtDecoder;
  private final JwtAuthenticationConverter authenticationConverter;
  private static final String BEARER_PREFIX = "Bearer ";
  private final WebsocketSessionStorage sessionStorage;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setUserDestinationPrefix("/user");

    registry.setApplicationDestinationPrefixes("/api");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .addInterceptors(new IpHandshakeInterceptor())
        .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    final var bearerInterceptor = BearerHandshakeInterceptor
                                      .customInstance(jwtDecoder, authenticationConverter,
                                          BEARER_PREFIX);
    final var sessionInterceptor = SessionManagementInterceptor
                                       .withStore(this.sessionStorage);

    registration.interceptors(bearerInterceptor, sessionInterceptor);
  }
}
