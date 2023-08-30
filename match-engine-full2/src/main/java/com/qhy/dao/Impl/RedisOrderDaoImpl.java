package com.qhy.dao.Impl;

import com.qhy.dao.RedisOrderDao;
import com.qhy.pojo.Order;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository("RedisOrderDao")
public class RedisOrderDaoImpl implements RedisOrderDao {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void setOrder(String orderId, Order order) {
        redisTemplate.opsForHash().put(Constant.Common.ORDER_LIST_KEY(), orderId, order);
    }

    @Override
    public Order getOrder(String orderId) {
        return (Order) redisTemplate.opsForHash().get(Constant.Common.ORDER_LIST_KEY(), orderId);
    }

    @Override
    public void deleteOrder(String orderId) {
        redisTemplate.opsForHash().delete(Constant.Common.ORDER_LIST_KEY(), orderId);
    }

    @Override
    public void setMaxOrderId(String id) {
        redisTemplate.opsForValue().set(Constant.Common.MAX_ORDER_ID_KEY(), id);
    }

    @Override
    public String getMaxOrderId() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(Constant.Common.MAX_ORDER_ID_KEY()))) {
            redisTemplate.opsForValue().set(Constant.Common.MAX_ORDER_ID_KEY(), "0");
            // Todo 自增
//            redisTemplate.opsForValue().increment();
            return "0";
        } else {
            return (String) redisTemplate.opsForValue().get(Constant.Common.MAX_ORDER_ID_KEY());
        }
    }

    @Override
    synchronized public Order handleMaxOrderId(Order order) {
        order.setOrderid(String.valueOf(Long.valueOf(this.getMaxOrderId())+1L));
        this.setMaxOrderId(order.getOrderid());
        return order;
    }
}
