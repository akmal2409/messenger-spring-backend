package com.akmal.messengerspringbackend.config.security;

import static net.logstash.logback.argument.StructuredArguments.v;

import com.akmal.messengerspringbackend.CorsConfigurationProperties;
import com.akmal.messengerspringbackend.config.condition.CorsEnableCondition;
import com.akmal.messengerspringbackend.controller.v1.rest.ThreadController;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 15/06/2022 - 20:03
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfiguration {

  @Order(Ordered.HIGHEST_PRECEDENCE + 2)
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeRequests(
            authorize ->
                authorize
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .antMatchers("/api/v1/users/{userId}/**")
                    .access("authentication.name == #userId")
                    .antMatchers(ThreadController.BASE_URL.concat("/{threadId}/**"))
                    .access("@threadService.isUserThreadParticipant(#userId, #threadId)")
                    .antMatchers("/ws/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt().jwtAuthenticationConverter(jwtAuthenticationConverter()))
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf()
        .disable()
        .build();
  }

  @Bean
  @Conditional(CorsEnableCondition.class)
  FilterRegistrationBean<CorsFilter> corsConfigurationSource(CorsConfigurationProperties props) {
    final var config = new CorsConfiguration();
    final var allowedOrigins = Optional.ofNullable(props.getAllowedOrigins())
                                           .orElse(new String[0]);
    final var allowedMethods = Optional.ofNullable(props.getAllowedMethods())
                                           .orElse(new String[0]);

    final var allowedHeaders = Optional.ofNullable(props.getAllowedHeaders())
                                   .orElse(new String[0]);

    config.setAllowedOrigins(Arrays.asList(allowedOrigins));
    config.setAllowedMethods(Arrays.asList(allowedMethods));
    config.setAllowedHeaders(Arrays.asList(allowedHeaders));
    config.setAllowCredentials(true);

    final var urlBasedConfigSource = new UrlBasedCorsConfigurationSource();

    urlBasedConfigSource.registerCorsConfiguration("/**", config);

    log.info(
        "Event {} in a class {} in module {} and component {}",
        v("event", "bootstrap_configuration"),
        this.getClass().getName(),
        v("module", "security"),
        v("component", "CORS")
    );

    final var bean = new FilterRegistrationBean<>(new CorsFilter(urlBasedConfigSource));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

    return bean;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    final var converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("uid");
    return converter;
  }
}
