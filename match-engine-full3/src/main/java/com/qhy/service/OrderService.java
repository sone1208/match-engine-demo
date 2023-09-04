package com.qhy.service;

import com.qhy.pojo.Order;
import com.qhy.pojo.UserRelatedInfo;


public interface OrderService {
    public UserRelatedInfo getUserRelatedOrders(Integer userId);
    public Order addOrder(Order order);
    public boolean cancelOrder(Integer orderId);
    public void sendChangeOrders();
}
