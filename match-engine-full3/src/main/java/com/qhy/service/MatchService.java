package com.qhy.service;

import com.qhy.pojo.Order;

import java.util.List;

public interface MatchService {
    public List<Order> getSubscribedOrders(String code);
    public void matchExecutor(String code);
    public void matchExecutorMuti(String code);
    public void sendChangeOrders(String code);
}
