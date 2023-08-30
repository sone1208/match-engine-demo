package com.qhy.dao;

import com.qhy.pojo.Order;

public interface RedisOrderDao {
    public void setOrder(String orderId, Order order);
    public Order getOrder(String orderId);
    public void deleteOrder(String orderId);

    public void setMaxOrderId(String id);
    public String getMaxOrderId();
    public Order handleMaxOrderId(Order order);
}
