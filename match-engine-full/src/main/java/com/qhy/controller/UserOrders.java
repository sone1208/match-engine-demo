package com.qhy.controller;

import com.qhy.pojo.MatchOrder;
import com.qhy.pojo.Order;

import java.util.List;
import java.util.Map;

public class UserOrders {

    public List<Order> takerOrders;
    public List<String> subscribedShares;
    public Map<String, List<Order>> subscribedOrders;
    public List<MatchOrder> matchOrders;
}
