package com.qhy.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

@Component
@ServerEndpoint("/websocket") //暴露的ws应用的路径
public class WebSocket {

    // 用来存储服务连接对象
    private static Map<String, Session> clientMap = new ConcurrentHashMap<>();

    /**
     * 客户端与服务端连接成功
     * @param session
     */
    @OnOpen
    public void onOpen(Session session){
        /*
            do something for onOpen
            与当前客户端连接成功时
         */
        clientMap.put(session.getId(),session);
    }

    /**
     * 客户端与服务端连接关闭
     * @param session
     */
    @OnClose
    public void onClose(Session session){
        /*
            do something for onClose
            与当前客户端连接关闭时
         */
        clientMap.remove(session.getId());
    }

    /**
     * 客户端与服务端连接异常
     * @param error
     * @param session
     */
    @OnError
    public void onError(Throwable error,Session session) {
        error.printStackTrace();
    }

    /**
     * 客户端向服务端发送消息
     * @param message
     * @throws IOException
     */
    @OnMessage
    public void onMsg(Session session,String message) throws IOException {
        //其余客户端刷新不由某个客户端前台信息决定，而是由后台redis监控决定，所以没有接收到消息后的处理，也不会有消息
    }

    public void onRedisUpdate() throws IOException {
        sendAllMessage("redis orderBook updated");
    }

    //向所有客户端发送消息（广播）
    private void sendAllMessage(String message){
        Set<String> sessionIdSet = clientMap.keySet(); //获得Map的Key的集合
        // 此处相当于一个广播操作
        for (String sessionId : sessionIdSet) { //迭代Key集合
            Session session = clientMap.get(sessionId); //根据Key得到value
            session.getAsyncRemote().sendText(message); //发送消息给客户端
        }
    }
}