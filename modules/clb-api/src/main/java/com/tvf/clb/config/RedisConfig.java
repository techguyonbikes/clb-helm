package com.tvf.clb.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvf.clb.base.entity.EntrantResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.util.List;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    @Qualifier
    public ReactiveRedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public ReactiveRedisTemplate<Long, List<EntrantResponseDto>> raceDetailTemplate(ReactiveRedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructParametricType(List.class, EntrantResponseDto.class);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(type);
        return new ReactiveRedisTemplate<Long, List<EntrantResponseDto>>(
                factory,
                RedisSerializationContext.fromSerializer(jackson2JsonRedisSerializer)
        );
    }

    @Bean
    public ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisTemplate<String, Long>(
                factory,
                RedisSerializationContext.fromSerializer(new Jackson2JsonRedisSerializer(Long.class)));
    }
}
