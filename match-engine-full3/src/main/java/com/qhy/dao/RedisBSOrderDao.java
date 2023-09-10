package com.qhy.dao;

import com.qhy.pojo.Order;

import java.util.List;
import java.util.Set;

public interface RedisBSOrderDao {
    public void addOrder(String code, Order order);
    public Set<Integer> getAllOrders(String code);
    public Set<Integer> getTopOrders(String code, Long topNum);
    public Integer getTopOneOrder(String code);
    public List<Integer> getTopOrdersByScore(String code, Order order);
    public void deleteOrder(String code, Order order);
    public void deleteOrders(String code, List<String> orderIds);
    public double getScore(Order order);
    public String getFullKey(String code);
}
