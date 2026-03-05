package com.pizzeriaservice.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  GroupedOpenApi v1Api() {
    return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1/**").build();
  }

  @Bean
  OpenAPI openApi(ObjectProvider<BuildProperties> buildPropertiesProvider) {
    BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
    String version = buildProperties != null ? buildProperties.getVersion() : "0.0.1";
    return new OpenAPI()
        .info(
            new Info()
                .title("Pizzeria Service API")
                .version(version)
                .description("Reactive API for the pizzeria platform")
                .contact(new Contact().name("Pizzeria Team")));
  }
}
