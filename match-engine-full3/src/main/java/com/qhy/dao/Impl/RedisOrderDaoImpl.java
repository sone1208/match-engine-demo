package com.qhy.dao.Impl;

import com.qhy.dao.RedisOrderDao;
import com.qhy.pojo.Order;
import com.qhy.util.Constant;
import org.springframework.beans.BeanInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanIteration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository("RedisOrderDao")
public class RedisOrderDaoImpl implements RedisOrderDao {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void putOrder(Integer orderId, Order order) {
        redisTemplate.opsForHash().put(Constant.Common.ORDER_LIST_KEY(), orderId, order);
    }

    @Override
    public void putOrders(Map<Integer, Order> orders) {
        redisTemplate.opsForHash().putAll(Constant.Common.ORDER_LIST_KEY(), orders);
    }

    @Override
    public Order getOrder(Integer orderId) {
        return (Order) redisTemplate.opsForHash().get(Constant.Common.ORDER_LIST_KEY(), orderId);
    }

    @Override
    public List<Order> getOrders(List<Integer> orderIds) {
        return redisTemplate.opsForHash().multiGet(Constant.Common.ORDER_LIST_KEY(), orderIds);
    }

    @Override
    public boolean changeOneOrderStatusByLua(Order order, Integer status) {
        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        // 指定 lua 脚本
        redisScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("static/lua/changeOneStatus.lua")));
        // 指定返回类型
        redisScript.setResultType(Boolean.class);
        // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
        return (Boolean) redisTemplate.execute(redisScript, Collections.singletonList(order.getOrderid()), status);
    }

    @Override
    public boolean changeTwoOrderStatusByLua(Order sellOrder, Order buyOrder) {
        List<Integer> orderIds = new ArrayList<>();
        orderIds.add(sellOrder.getOrderid());
        orderIds.add(buyOrder.getOrderid());

        DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
        // 指定 lua 脚本
        redisScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("static/lua/changeTwoStatus.lua")));
        // 指定返回类型
        redisScript.setResultType(Boolean.class);
        // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
        return (Boolean) redisTemplate.execute(redisScript, orderIds);
    }

}
