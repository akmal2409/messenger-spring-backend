package com.akmal.messengerspringbackend.config.security;

import com.akmal.messengerspringbackend.controller.v1.rest.MessageController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
@EnableWebSecurity
public class SecurityConfiguration {

  @Order(1)
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeRequests(authorize -> authorize
                                                   .antMatchers(MessageController.BASE_API).access("authentication.name == #userId")
                                                   .antMatchers("/ws/**").permitAll()
                                                   .anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt().jwtAuthenticationConverter(jwtAuthenticationConverter()))
               .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(
                   SessionCreationPolicy.STATELESS))
        .csrf()
        .disable()
        .cors()
        .disable()
        .build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    final var converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("uid");
    return converter;
  }
}
