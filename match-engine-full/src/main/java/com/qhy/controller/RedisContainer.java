package com.qhy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Component
public class RedisContainer {

    @Autowired
    RedisListener redisListener;
    @Autowired
    private TaskExecutor redisListenerContainerTaskExecutor;

    private static final Topic TOPIC_ORDER_KEYSPACE = new PatternTopic("__keyspace@*__:\"ORDER\"");

    //只需要捕获orderBook的变化即可
    private String keyspaceNotificationsConfigParameter = "Kh";

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        //配置container和listener
        container.setConnectionFactory(factory);
        container.addMessageListener(redisListener, TOPIC_ORDER_KEYSPACE);
        container.setTaskExecutor(redisListenerContainerTaskExecutor);

        //配置键空间通知功能
        if (StringUtils.hasText(keyspaceNotificationsConfigParameter)) {
            RedisConnection connection = container.getConnectionFactory().getConnection();
            try {
                Properties config = connection.getConfig("notify-keyspace-events");
                if (!StringUtils.hasText(config.getProperty("notify-keyspace-events"))) {
                    connection.setConfig("notify-keyspace-events", keyspaceNotificationsConfigParameter);
                }
            } finally {
                connection.close();
            }
        }

        return container;
    }
}

