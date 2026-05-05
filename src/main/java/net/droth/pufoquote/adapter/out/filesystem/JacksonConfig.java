package net.droth.pufoquote.adapter.out.filesystem;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
class JacksonConfig {

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
