package net.droth.pufoquote.adapter.out.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
class RedisConfig {

  @Bean
  RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    StringRedisSerializer str = new StringRedisSerializer();
    template.setKeySerializer(str);
    template.setValueSerializer(str);
    template.setHashKeySerializer(str);
    template.setHashValueSerializer(str);
    return template;
  }
}
