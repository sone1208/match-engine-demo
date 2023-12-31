package com.qhy.dao.Impl;

import com.qhy.dao.RedisBSOrderDao;
import com.qhy.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Repository("RedisOrderSellBookDao")
public class RedisSellOrderDaoImpl implements RedisBSOrderDao {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addOrder(String code, Order order) {
        String orderIdKey = String.format("%" + 12 + "s", order.getOrderid())
                .replace(' ', '0');
        redisTemplate.opsForZSet().add(getFullKey(code), orderIdKey, getScore(order));
    }

    @Override
    public Set<String> getAllOrders(String code) {
        Set<String> orders = redisTemplate.opsForZSet().range(getFullKey(code), 0, -1);
        Set<String> res = new HashSet<>();
        for (String order : orders) {
            res.add(order.replaceAll("^(0+)", ""));
        }
        return res;
    }

    @Override
    public Set<String> getTopOrders(String code, Long topNum) {
        Set<String> orders;
        if (redisTemplate.opsForZSet().zCard(getFullKey(code)) < topNum) {
            orders =  redisTemplate.opsForZSet().range(getFullKey(code), 0, -1);
        } else {
            orders =  redisTemplate.opsForZSet().range(getFullKey(code), 0, topNum-1);
        }

        Set<String> res = new HashSet<>();
        for (String order : orders) {
            res.add(order.replaceAll("^(0+)", ""));
        }
        return res;
    }

    @Override
    public String getTopOneOrder(String code) {
        if (redisTemplate.opsForZSet().size(getFullKey(code)) > 0) {
            Set<String> codes = redisTemplate.opsForZSet().range(getFullKey(code), 0, 0);
            for (String topCode : codes) {
                return topCode.replaceAll("^(0+)", "");
            }
        }
        return null;
    }

    @Override
    public void deleteOrder(String code, Order order) {
        redisTemplate.opsForZSet().remove(getFullKey(code),
                String.format("%" + 12 + "s", order.getOrderid())
                .replace(' ', '0'));
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
        return "SELL_"+code;
    }
}
