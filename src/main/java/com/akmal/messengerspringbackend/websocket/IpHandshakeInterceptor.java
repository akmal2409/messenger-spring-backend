package com.akmal.messengerspringbackend.websocket;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 22/06/2022 - 19:08
 * @project messenger-spring-backend
 * @since 1.0
 */
public class IpHandshakeInterceptor implements HandshakeInterceptor {
  public static final String IP_ATTR_ACCESSOR = "remote_address";

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
    attributes.put(IP_ATTR_ACCESSOR, request.getRemoteAddress());
    return true;
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {

  }
}
