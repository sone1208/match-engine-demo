package com.qhy.service;

import com.qhy.pojo.MatchOrder;
import com.qhy.pojo.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Transactional
public interface OrderService {
    //展示当前账户委托信息
    public List<Order> getTakerOrders(Integer user_id);
    //展示当前用户订阅的股票
    public List<String> getSubscribedShares(Integer user_id);
    //展示当前用户订阅信息
    public Map<String, List<Order>> getSubscribedOrders(Integer user_id);
    //展示当前用户的成交信息
    public List<MatchOrder> getMatchOrder(Integer user_id);
    //新增订单
    public void addOrder(Order order);
    //取消订单
    public boolean cancelOrder(String order_id);
}
