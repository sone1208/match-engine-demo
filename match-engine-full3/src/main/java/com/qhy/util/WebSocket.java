package com.qhy.util;

import com.qhy.pojo.Order;
import com.qhy.pojo.Results;
import com.qhy.pojo.TradingRecord;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint(value = "/websocket", encoders = { ServerEncoder.class })
public class WebSocket {

    // 用来存储服务连接对象
    private static Map<String, Session> clientMap = new ConcurrentHashMap<>();
    // 一个用户id对应的sessionId集合
    private static Map<Integer, List<String>> userIdMapSession = new ConcurrentHashMap<>();
    // 一支股票对应的订阅者sessionId集合
    private static Map<String, List<String>> subscribedStockIdMapSession = new ConcurrentHashMap<>();
    // sessionId向userId的映射
    private static Map<String, Integer> sessionMapUserId = new ConcurrentHashMap<>();
    // sessionId向订阅股票的映射
    private static Map<String, String> sessionMapSubscribedStockId = new ConcurrentHashMap<>();

    private Session session;

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
        log.info("开启websocket连接");
        this.session = session;
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
        log.info("关闭websocket连接");
        clientMap.remove(session.getId());

        Integer userId = sessionMapUserId.get(session.getId());
        sessionMapUserId.remove(session.getId());
        if (userId != null) {
            userIdMapSession.get(userId).remove(session.getId());
        }


        String stockId = sessionMapSubscribedStockId.get(session.getId());
        sessionMapSubscribedStockId.remove(session.getId());
        if (stockId != null) {
            subscribedStockIdMapSession.get(stockId).remove(session.getId());
        }

    }

    /**
     * 客户端与服务端连接异常
     * @param error
     * @param session
     */
    @OnError
    public void onError(Throwable error,Session session) {
        log.error("websocket出错"+error.toString());
        error.printStackTrace();
    }

    /**
     * 客户端向服务端发送消息
     * @param message
     * @throws IOException
     */
    @OnMessage
    public void onMsg(Session session,String message) throws IOException {
        /**
         * 客户端向服务器发送消息，通过tomcat下的请求响应模式进行
         * websocket只用来处理服务端向客户端主动推消息的需求
         */
        log.info("收到消息：" + message);
        if (message.contains("newUser----")) {
            Integer userId = Integer.valueOf(message.split("----")[1]);
            setUserIdMap(userId, session);
        } else if (message.contains("delUser----")) {
            deleteUserIdMap(session);
        } else if (message.contains("newCode----")) {
            String code = message.split("----")[1];
            setSubscribedOrderIdMap(code, session);
        } else if (message.contains("delCode----")) {
            deleteSubscribedOrderIdMap(session);
        } else {
            log.warn("error message !");
        }
    }

    public void setUserIdMap(Integer userId, Session session) {
        log.info("设置session："+ session.getId() +"和用户id: " + userId + "的映射");
        userIdMapSession.computeIfAbsent(userId, key -> new ArrayList<>()).add(session.getId());
        sessionMapUserId.put(session.getId(), userId);
    }

    public void deleteUserIdMap(Session session) {
        log.info("删除session: " + session.getId() + "和用户id的映射");
        Integer userId = sessionMapUserId.get(session.getId());
        if (userId == null)
            return ;
        sessionMapUserId.remove(session.getId());
        userIdMapSession.get(userId).remove(session.getId());
    }

    public void setSubscribedOrderIdMap(String code, Session session) {
        log.info("设置session："+ session.getId() +"和股票代码: " + code + "的映射");
        subscribedStockIdMapSession.computeIfAbsent(code, key -> new ArrayList<>()).add(session.getId());
        sessionMapSubscribedStockId.put(session.getId(), code);
    }

    public void deleteSubscribedOrderIdMap(Session session) {
        log.info("删除session: " + session.getId() + "和股票代码的映射");
        String stockId = sessionMapSubscribedStockId.get(session.getId());
        if (stockId == null)
            return ;
        sessionMapSubscribedStockId.remove(session.getId());
        subscribedStockIdMapSession.get(stockId).remove(session.getId());
    }

    /**
     * 向订阅了当前股票的所有session发送变动的行情表
     * @param code 当前订阅股票
     * @throws IOException
     */
    public void sendNewSubscribedOrders(List<Order> subscribedOrders, String code) throws IOException {
        log.info("websocket向订阅了股票" + code + "的客户端发送新的订阅信息");
        List<String> sessionIds = subscribedStockIdMapSession.get(code);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                log.info("发送消息： " + sessionId + "----" + "subscribedOrders");
                Session session = clientMap.get(sessionId);
                session.getAsyncRemote().sendObject(new Results("subscribedOrders", subscribedOrders));
            }
        }
    }

    /**
     * 当某用户新增订单时，将新订单广播给对应用户的session连接
     * @param userId 新增订单的用户
     * @throws IOException
     */
    public void sendNewTakerOrders(List<Order> takerOrders, Integer userId) throws IOException {
        log.info("websocket向用户id为" + userId + "的客户端发送新的订单委托信息");
        List<String> sessionIds = userIdMapSession.get(userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                log.info("发送消息： " + sessionId + "----" + "newTakerOrders");
                Session session = clientMap.get(sessionId);
                session.getAsyncRemote().sendObject(new Results("newTakerOrders", takerOrders));
            }
        }
    }

    /**
     * 当某用户委托订单状态变化时，将发生变化的订单广播给对应用户的session连接
     * @param userId 拥有委托发生变动的用户
     * @throws IOException
     */
    public void sendChangeTakerOrders(List<Order> takerOrders, Integer userId) throws IOException {
        log.info("websocket向用户id为" + userId + "的客户端发送更改的订单信息");
        List<String> sessionIds = userIdMapSession.get(userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                log.info("发送消息： " + sessionId + "----" + "changeTakerOrders");
                Session session = clientMap.get(sessionId);
                session.getAsyncRemote().sendObject(new Results("changeTakerOrders", takerOrders));
            }
        }
    }

    /**
     * 当成交记录生成后，向拥有的用户推送对应的消息
     * @param userId 成交记录拥有者id
     * @throws IOException
     */
    public void sendNewTradingRecords(List<TradingRecord> tradingRecords, Integer userId) throws IOException {
        log.info("websocket向用户id为" + userId + "的客户端发送新的成交记录");
        List<String> sessionIds = userIdMapSession.get(userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                log.info("发送消息： " + sessionId + "----" + "tradingRecords");
                Session session = clientMap.get(sessionId);
                session.getAsyncRemote().sendObject(new Results("tradingRecords", tradingRecords));
            }
        }
    }

    /**
     * 向所有客户端发送消息（广播）
     * @param message 传送的消息
     * @throws IOException
     */
    public void sendAllMessage(String message) throws IOException {
        log.info("广播消息"+message);
        Set<String> sessionIdSet = clientMap.keySet(); //获得Map的Key的集合
        // 此处相当于一个广播操作
        for (String sessionId : sessionIdSet) { //迭代Key集合
            Session session = clientMap.get(sessionId); //根据Key得到value
            session.getAsyncRemote().sendText(message); //发送消息给客户端
        }
    }
}