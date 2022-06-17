package com.akmal.messengerspringbackend.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 15/06/2022 - 20:03
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeRequests(authorize -> authorize.anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt().jwtAuthenticationConverter(jwtAuthenticationConverter()))
        .csrf()
        .disable()
        .cors()
        .disable()
        .build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    final var converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("uid");
    return converter;
  }
}
