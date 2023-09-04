package com.qhy.dao;

import com.qhy.pojo.Order;

import java.util.List;

public interface RedisOrderDao {
    public void putOrder(Integer orderId, Order order);
    public Order getOrder(Integer orderId);
    public boolean changeOneOrderStatusByLua(Order order);
    public boolean changeTwoOrderStatusByLua(Order sellOrder, Order buyOrder);
}
