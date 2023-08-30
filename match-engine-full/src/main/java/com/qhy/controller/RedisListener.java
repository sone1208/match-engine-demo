package com.qhy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class RedisListener implements MessageListener {

    @Autowired
    WebSocket webSocket;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        /*
        * 监控到redis变化就操作websocket向客户端广播消息
        * */
        try {
            webSocket.onRedisUpdate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

