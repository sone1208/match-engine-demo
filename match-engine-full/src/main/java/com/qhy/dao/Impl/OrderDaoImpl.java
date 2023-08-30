package com.qhy.dao.Impl;

import com.alibaba.fastjson.JSON;
import com.qhy.dao.OrderDao;
import com.qhy.pojo.Order;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository("OrderDaoImpl")
public class OrderDaoImpl implements OrderDao {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void setKey(String order_id, Order order) {
        redisTemplate.opsForHash().put(Constant.Common.ORDER_BOOK(), order_id, order);
    }

    @Override
    public Order getValue(String order_id) {
        return (Order) redisTemplate.opsForHash().get(Constant.Common.ORDER_BOOK(), order_id);
    }

    @Override
    public void deleteKey(String order_id) {
        redisTemplate.opsForHash().delete(Constant.Common.ORDER_BOOK(), order_id);
    }

    @Override
    public void setMaxOrderId(String id) {
        redisTemplate.opsForValue().set(Constant.Common.MAX_ORDER_ID_NAME(), id);
    }

    @Override
    public String getMaxOrderId() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(Constant.Common.MAX_ORDER_ID_NAME())))
            redisTemplate.opsForValue().set(Constant.Common.MAX_ORDER_ID_NAME(), "0");
        return (String) redisTemplate.opsForValue().get(Constant.Common.MAX_ORDER_ID_NAME());
    }
}
