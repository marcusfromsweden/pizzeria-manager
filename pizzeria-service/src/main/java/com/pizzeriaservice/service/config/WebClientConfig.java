package com.pizzeriaservice.service.config;

import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean
  WebClient baseWebClient() {
    return WebClient.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .clientConnector(
            new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(Duration.ofSeconds(10))))
        .filter(correlationIdFilter())
        .build();
  }

  @Bean
  WebClient downstreamClient(WebClient baseWebClient) {
    return baseWebClient
        .mutate()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  private ExchangeFilterFunction correlationIdFilter() {
    return (request, next) -> {
      if (request.headers().containsKey("X-Correlation-Id")) {
        return next.exchange(request);
      }
      ClientRequest filtered =
          ClientRequest.from(request)
              .header("X-Correlation-Id", UUID.randomUUID().toString())
              .build();
      return next.exchange(filtered);
    };
  }
}
