package com.qhy.dao.Impl;

import com.qhy.dao.OrderBookDao;
import com.qhy.pojo.Order;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Repository("OrderSellBookDaoImpl")
public class OrderSellBookDaoImpl implements OrderBookDao {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addOrder(String share_id, Order order) {
        redisTemplate.opsForZSet().add(getFullKey(share_id), order.getOrder_id(), getScore(order));
    }

    @Override
    public Set<String> getAllOrders(String share_id) {
        return redisTemplate.opsForZSet().range(getFullKey(share_id), 0, -1);
    }

    @Override
    public Set<String> getTopOrders(String share_id, Long topNum) {
        if (redisTemplate.opsForZSet().zCard(getFullKey(share_id)) < topNum) {
            return redisTemplate.opsForZSet().range(getFullKey(share_id), 0, -1);
        } else {
            return redisTemplate.opsForZSet().range(getFullKey(share_id), 0, topNum-1);
        }
    }

    @Override
    public void deleteOrder(String share_id, Order order) {
        redisTemplate.opsForZSet().remove(getFullKey(share_id), order.getOrder_id());
    }

    @Override
    public double getScore(Order order) {
        int orderIdLen = order.getOrder_id().length();

        double orderPrice = order.getPrice().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        double orderId = Double.valueOf(order.getOrder_id());
        double score = orderPrice + orderId * Math.pow(10.0, -orderIdLen);

        return score;
    }

    @Override
    public String getFullKey(String share_id) {
        return "SELL-"+share_id;
    }

}
