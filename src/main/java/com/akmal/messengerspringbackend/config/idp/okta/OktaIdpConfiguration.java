package com.akmal.messengerspringbackend.config.idp.okta;

import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.cache.Caches;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.user.User;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Akmal Alikhujaev
 * @version 1.0
 * @created 18/06/2022 - 17:56
 * @project messenger-spring-backend
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class OktaIdpConfiguration {
  private final OktaIdpConfigurationProperties oktaProps;

  @Bean
  Client oktaClient() {
    final var cacheManager =
        Caches.newCacheManager()
            .withDefaultTimeToLive(300, TimeUnit.SECONDS)
            .withDefaultTimeToIdle(300, TimeUnit.SECONDS)
            .withCache(
                Caches.forResource(User.class)
                    .withTimeToLive(2, TimeUnit.HOURS)
                    .withTimeToIdle(1, TimeUnit.MINUTES))
            .build();

    return Clients.builder()
        .setOrgUrl(this.oktaProps.getOrgUrl())
        .setCacheManager(cacheManager)
        .setClientCredentials(new TokenClientCredentials(this.oktaProps.getApiKey()))
        .build();
  }
}
