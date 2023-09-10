package com.qhy.dao.Impl;

import com.qhy.dao.RedisBSOrderDao;
import com.qhy.pojo.Order;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository("RedisOrderSellBookDao")
public class RedisSellOrderDaoImpl implements RedisBSOrderDao {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addOrder(String code, Order order) {
        String orderIdKey = String.format("%012d", order.getOrderid());
        redisTemplate.opsForZSet().add(getFullKey(code), orderIdKey, getScore(order));
    }

    @Override
    public Set<Integer> getAllOrders(String code) {
        Set<String> orders = redisTemplate.opsForZSet().range(getFullKey(code), 0, -1);
        Set<Integer> res = new HashSet<>();
        for (String order : orders) {
            res.add(Integer.parseInt(order));
        }
        return res;
    }

    @Override
    public Set<Integer> getTopOrders(String code, Long topNum) {
        Set<String> orders;
        if (redisTemplate.opsForZSet().zCard(getFullKey(code)) < topNum) {
            orders =  redisTemplate.opsForZSet().range(getFullKey(code), 0, -1);
        } else {
            orders =  redisTemplate.opsForZSet().range(getFullKey(code), 0, topNum-1);
        }

        Set<Integer> res = new HashSet<>();
        for (String order : orders) {
            res.add(Integer.parseInt(order));
        }
        return res;
    }

    @Override
    public Integer getTopOneOrder(String code) {
        Set<String> orderIds = redisTemplate.opsForZSet().range(getFullKey(code), 0, 0);
        if (orderIds.isEmpty())
            return null;
        for (String topOrderId : orderIds) {
            return Integer.parseInt(topOrderId);
        }
        return null;
    }

    @Override
    public List<Integer> getTopOrdersByScore(String code, Order order) {
        //取出来的订单一定要么跟order的分数相同，要么更优
        Set<String> orderIds = redisTemplate.opsForZSet().rangeByScore(getFullKey(code),
                -1.0, getScore(order));
        if (orderIds.isEmpty())
            return null;

        List<Integer> res = new ArrayList<>();
        Integer invalidIdLow = Integer.MAX_VALUE;
        boolean hasScanTopId = false;

        for (String topOrderId : orderIds) {
            Integer orderId = Integer.parseInt(topOrderId);
            // 排序在order以前的一定是价格更优的，此时直接忽略，并记录忽略的order里面id最小的记为invalidIdLow
            if (!hasScanTopId) {
                if (orderId > order.getOrderid()) {
                    invalidIdLow = Integer.min(invalidIdLow, orderId);
                } else if (orderId.equals(order.getOrderid())) {
                    hasScanTopId = true;
                    res.add(orderId);
                }
            } else {
                //已经扫描过了最开始提取的orderId，后面的order如果有超过invalidIdLow的也忽略掉，因为逻辑上比更优的晚，不能处理
                if (orderId > invalidIdLow) {
                    break;
                } else {
                    res.add(orderId);
                }
            }
        }
        if (res.isEmpty())
            return null;

        return res;
    }

    @Override
    public void deleteOrder(String code, Order order) {
        redisTemplate.opsForZSet().remove(getFullKey(code), String.format("%012d", order.getOrderid()));
    }

    @Override
    public void deleteOrders(String code, List<String> orderIds) {
        redisTemplate.opsForZSet().remove(getFullKey(code), orderIds.toArray());
    }

    /**
     * 精度为小数点后6位，结果通过x1000000将小数转为整数
     * @param order 输入的order
     * @return order的score
     */
    @Override
    public double getScore(Order order) {
        BigDecimal orderPrice = order.getPrice()
                .multiply(new BigDecimal(1000000))
                .setScale(0, BigDecimal.ROUND_HALF_UP);

        return orderPrice.doubleValue();
    }

    @Override
    public String getFullKey(String code) {
        return Constant.Common.SELL_ORDER_LIST_KEY_PRE() + code;
    }
}
