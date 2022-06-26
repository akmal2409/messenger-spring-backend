package com.akmal.messengerspringbackend.config.security;

import com.akmal.messengerspringbackend.config.websocket.WebSocketConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 17/06/2022 - 19:20
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
public class WebsocketSecurityConfiguration
    extends AbstractSecurityWebSocketMessageBrokerConfigurer {

  @Override
  protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
    messages
        .simpDestMatchers("/queue/**", WebSocketConfiguration.THREAD_TOPIC).denyAll()
        .simpDestMatchers(WebSocketConfiguration.THREAD_TOPIC + "/{threadId}").access("@userService.currentUser.threadIds.contains(T(java.util.UUID).fromString(#threadId))")
        .anyMessage().authenticated();
  }

  @Override
  protected boolean sameOriginDisabled() {
    return true;
  }
}
