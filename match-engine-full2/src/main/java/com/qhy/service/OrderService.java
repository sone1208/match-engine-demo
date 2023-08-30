package com.qhy.service;

import com.qhy.pojo.Order;
import com.qhy.pojo.TradingRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Transactional
public interface OrderService {

    //记录某一支股票是否正在被处理
    Map<String, Boolean> stockExecutingStatus = new ConcurrentHashMap<>();

    public List<Order> getTakerOrders(Integer userId);
    public List<Order> getSubscribedOrders(String code);
    public List<TradingRecord> getTradingOrder(Integer userId);
    public void addOrder(Order order);
    public boolean cancelOrder(String orderId);
}
