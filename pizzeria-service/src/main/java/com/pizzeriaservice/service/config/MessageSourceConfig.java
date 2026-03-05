package com.pizzeriaservice.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {

  @Bean
  MessageSource messageSource(
      @Value("${service.messages.basename:classpath:messages}") String basename) {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames(basename);
    source.setDefaultEncoding("UTF-8");
    source.setCacheSeconds(3600);
    return source;
  }
}
