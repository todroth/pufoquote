package net.droth.pufoquote.adapter.out.groq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
class GroqConfig {

  @Bean
  WebClient groqWebClient(@Value("${groq.api-key}") String apiKey) {
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(
                c -> c.defaultCodecs().maxInMemorySize((int) DataSize.ofMegabytes(16).toBytes()))
            .build();
    return WebClient.builder()
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .exchangeStrategies(strategies)
        .build();
  }
}
