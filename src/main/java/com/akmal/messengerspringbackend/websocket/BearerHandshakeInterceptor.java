package com.akmal.messengerspringbackend.websocket;

import com.akmal.messengerspringbackend.exception.AuthorizationException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 17/06/2022 - 19:25
 * @project messenger-spring-backend
 * @since 1.0
 */
@Slf4j
public class BearerHandshakeInterceptor implements ChannelInterceptor {

  private final JwtDecoder jwtDecoder;
  private String bearerPrefix;

  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  private BearerHandshakeInterceptor(JwtDecoder jwtDecoder) {
    this(jwtDecoder, new JwtAuthenticationConverter(), "Bearer ");
  }

  private BearerHandshakeInterceptor(
      JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter, String prefix) {
    this.jwtDecoder = Objects.requireNonNull(jwtDecoder, "JwtDecoder was null");
    this.jwtAuthenticationConverter = Objects.requireNonNull(jwtAuthenticationConverter, "Jwt authentication converter was null");
    this.bearerPrefix = Objects.requireNonNull(prefix, "Bearer token header prefix was null");

    if (this.bearerPrefix.length() > 0
            && !Character.isSpaceChar(this.bearerPrefix.charAt(this.bearerPrefix.length() - 1))) {
      this.bearerPrefix = this.bearerPrefix.concat(" ");
    }
  }

  public static ChannelInterceptor withJwtDecoder(JwtDecoder jwtDecoder) {
    return new BearerHandshakeInterceptor(jwtDecoder);
  }

  public static ChannelInterceptor withCustomBearerPrefix(JwtDecoder jwtDecoder, String prefix) {
    return new BearerHandshakeInterceptor(jwtDecoder, new JwtAuthenticationConverter(), prefix);
  }

  public static ChannelInterceptor customInstance(
      JwtDecoder jwtDecoder, JwtAuthenticationConverter converter, String prefix) {
    return new BearerHandshakeInterceptor(jwtDecoder, converter, prefix);
  }

  @Override
  public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
    final StompHeaderAccessor headerAccessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    Objects.requireNonNull(headerAccessor, "STOMP header accessor was null on the inbound message");
    if (StompCommand.CONNECT.equals(headerAccessor.getCommand())) {
      final var accessToken = this.extractAccessToken(headerAccessor);

      final Jwt jwt = this.jwtDecoder.decode(accessToken);
      final Authentication authentication = this.jwtAuthenticationConverter.convert(jwt);
      headerAccessor.setUser(authentication);
    }

    return message;
  }

  private String extractAccessToken(StompHeaderAccessor headerAccessor) {
    final var authorizationHeaderValues = headerAccessor.getNativeHeader("Authorization");
    if (authorizationHeaderValues == null || authorizationHeaderValues.isEmpty()) {
      throw new AuthorizationException("Bearer token is missing in the websocket handshake header");
    }

    final var bearerToken = authorizationHeaderValues.get(0);

    if (!bearerToken.startsWith(this.bearerPrefix)) {
      throw new AuthorizationException(
          "Bearer prefix does not follow predefined pattern. Expected prefix " + this.bearerPrefix);
    }

    return bearerToken.split(" ")[1];
  }
}
