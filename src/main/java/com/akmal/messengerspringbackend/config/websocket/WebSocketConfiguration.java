package com.akmal.messengerspringbackend.config.websocket;

import com.akmal.messengerspringbackend.service.UserPresenceService;
import com.akmal.messengerspringbackend.websocket.BearerHandshakeInterceptor;
import com.akmal.messengerspringbackend.websocket.IpHandshakeInterceptor;
import com.akmal.messengerspringbackend.websocket.SessionManagementInterceptor;
import com.akmal.messengerspringbackend.websocket.storage.WebsocketSessionStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
  public static final String NOTIFICATION_TOPIC = "/user/queue/notifications";
  public static final String THREAD_MESSAGE_ACK_TOPIC = "/user/queue/threads/{threadId}/acks";
  public static final String ERROR_TOPIC = "/user/queue/errors";
  public static final String THREAD_TOPIC =
      "/user/queue/threads";
  private static final String BEARER_PREFIX = "Bearer ";
  private final JwtDecoder jwtDecoder;
  private final JwtAuthenticationConverter authenticationConverter;
  private final WebsocketSessionStorage sessionStorage;
  private final UserPresenceService userPresenceService;
  @Qualifier("asyncExecutor") private final AsyncTaskExecutor asyncTaskExecutor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue")
        .setTaskScheduler(heartBeatScheduler());
    registry.setUserDestinationPrefix("/user");

    registry.setApplicationDestinationPrefixes("/ws-api");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .addInterceptors(new IpHandshakeInterceptor());
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    final var bearerInterceptor =
        BearerHandshakeInterceptor.customInstance(
            jwtDecoder, authenticationConverter, BEARER_PREFIX);
    final var sessionInterceptor = SessionManagementInterceptor.withStore(this.sessionStorage);

    sessionInterceptor.registerPostConnectCallback(
        session -> {
          this.asyncTaskExecutor.submit(
              () -> this.userPresenceService.sendUserPresenceEvent(session.uid()));
        });

    registration.interceptors(bearerInterceptor, sessionInterceptor);
  }

  @Bean
  public TaskScheduler heartBeatScheduler() {
    return new ThreadPoolTaskScheduler();
  }
}
