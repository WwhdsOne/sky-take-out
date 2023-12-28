package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建redis模板测试...");
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        //设置redis key序列化器
        //新序列化器使图形化界面看起来更美观
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
