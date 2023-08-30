package com.qhy.dao;

import com.qhy.pojo.Order;

import java.util.Set;

public interface OrderBookDao {
    public void addOrder(String share_id, Order order);
    public Set<String> getAllOrders(String share_id);
    public Set<String> getTopOrders(String share_id, Long topNum);
    public void deleteOrder(String share_id, Order order);
    public double getScore(Order order);
    public String getFullKey(String share_id);
}
