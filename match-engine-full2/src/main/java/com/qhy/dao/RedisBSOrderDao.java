package com.qhy.dao;

import com.qhy.pojo.Order;

import java.util.Set;

public interface RedisBSOrderDao {
    public void addOrder(String code, Order order);
    public Set<String> getAllOrders(String code);
    public Set<String> getTopOrders(String code, Long topNum);
    public String getTopOneOrder(String code);
    public void deleteOrder(String code, Order order);
    public double getScore(Order order);
    public String getFullKey(String code);
}
