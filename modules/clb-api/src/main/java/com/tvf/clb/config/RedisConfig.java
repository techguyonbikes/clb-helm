package com.tvf.clb.config;

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
        return new ReactiveRedisTemplate<Long, List<EntrantResponseDto>>(
                factory,
                RedisSerializationContext.fromSerializer(new Jackson2JsonRedisSerializer(List.class))
        );
    }

    @Bean
    public ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisTemplate<String, Long>(
                factory,
                RedisSerializationContext.fromSerializer(new Jackson2JsonRedisSerializer(Long.class)));
    }
}