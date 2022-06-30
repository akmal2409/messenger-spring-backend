package com.akmal.messengerspringbackend.websocket;

import static net.logstash.logback.argument.StructuredArguments.v;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * The class below is an implementation of {@link HandshakeInterceptor} spring interface. Its main
 * purpose is to intercept the call and store in a request context the IP address of the sender.
 *
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 22/06/2022 - 19:08
 * @project messenger-spring-backend
 * @since 1.0
 */
@Slf4j
public class IpHandshakeInterceptor implements HandshakeInterceptor {
  public static final String IP_ATTR_ACCESSOR = "remote_address";

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes)
      throws Exception {

    log.debug(
        "Event {}. Received incoming connection over the {} protocol and sub-protocol {} at the stage {} "
            + "with remote address {}",
        v("event", "connect"),
        v("protocol", "websocket"),
        v("sub_protocol", null),
        v("connection_stage", "handshake"),
        v("ip_address", request.getRemoteAddress().getHostString()));

    attributes.put(IP_ATTR_ACCESSOR, request.getRemoteAddress());
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
