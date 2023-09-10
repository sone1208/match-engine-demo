package com.qhy.dao;

import com.qhy.pojo.Order;

import java.util.List;
import java.util.Map;

public interface RedisOrderDao {
    public void putOrder(Integer orderId, Order order);
    public void putOrders(Map<Integer, Order> orders);
    public Order getOrder(Integer orderId);
    public List<Order> getOrders(List<Integer> orderIds);
    public boolean changeOneOrderStatusByLua(Order order, Integer status);
    public boolean changeTwoOrderStatusByLua(Order sellOrder, Order buyOrder);
}
