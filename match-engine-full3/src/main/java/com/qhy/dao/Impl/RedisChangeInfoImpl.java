package com.qhy.dao.Impl;

import com.qhy.dao.RedisChangeInfo;
import com.qhy.pojo.MatchRecord;
import com.qhy.pojo.Order;
import com.qhy.pojo.TradingRecord;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 用来记录撮合过程中，产生的需要周期性落库的信息集合
 */
@Repository("RedisChangeInfo")
public class RedisChangeInfoImpl implements RedisChangeInfo {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addMatchRecord(String code, MatchRecord matchRecord) {
        redisTemplate.opsForList().rightPush(Constant.Common.TMP_MATCH_RECORDS_KEY()+code, matchRecord);
    }

    @Override
    public List<MatchRecord> getAndDeleteMatchRecords(String code) {
        Long len = redisTemplate.opsForList().size(Constant.Common.TMP_MATCH_RECORDS_KEY()+code);
        if (len == null || len == 0)
            return new ArrayList<>();
        else {
            List<MatchRecord> res = redisTemplate.opsForList().range(Constant.Common.TMP_MATCH_RECORDS_KEY()+code,
                    0, len-1);
            redisTemplate.opsForList().trim(Constant.Common.TMP_MATCH_RECORDS_KEY()+code, len, -1);
            return res;
        }
    }

    @Override
    public void addTradingRecord(String code, TradingRecord tradingRecord) {
        redisTemplate.opsForList().rightPush(Constant.Common.TMP_TRADING_RECORDS_KEY()+code, tradingRecord);
    }

    @Override
    public List<TradingRecord> getAndDeleteTradingRecords(String code, Long len) {
        if (len == null || len == 0)
            return new ArrayList<>();
        else {
            List<TradingRecord> res = redisTemplate.opsForList().range(Constant.Common.TMP_TRADING_RECORDS_KEY()+code,
                    0, len-1);
            redisTemplate.opsForList().trim(Constant.Common.TMP_TRADING_RECORDS_KEY()+code, len, -1);
            return res;
        }
    }

    @Override
    public void addTakerOrder(String code, Order order) {
        redisTemplate.opsForList().rightPush(Constant.Common.TMP_TAKER_ORDERS_KEY()+code, order);
    }

    @Override
    public List<Order> getAndDeleteTakerOrders(String code, Long len) {
        if (len == null || len == 0)
            return new ArrayList<>();
        else {
            List<Order> res = redisTemplate.opsForList().range(Constant.Common.TMP_TAKER_ORDERS_KEY()+code,
                    0, len-1);
            redisTemplate.opsForList().trim(Constant.Common.TMP_TAKER_ORDERS_KEY()+code, len, -1);
            return res;
        }
    }

    @Override
    public void addNewTakerOrder(Order order) {
        redisTemplate.opsForList().rightPush(Constant.Common.TMP_NEW_TAKER_ORDERS_KEY(), order);
    }

    @Override
    public List<Order> getAndDeleteNewTakerOrders() {
        Long len = redisTemplate.opsForList().size(Constant.Common.TMP_NEW_TAKER_ORDERS_KEY());
        if (len == null || len == 0)
            return new ArrayList<>();
         else {
            List<Order> res = redisTemplate.opsForList().range(Constant.Common.TMP_NEW_TAKER_ORDERS_KEY(),
                    0, len-1);
            redisTemplate.opsForList().trim(Constant.Common.TMP_NEW_TAKER_ORDERS_KEY(), len, -1);
            return res;
        }
    }

    @Override
    public void addCancelTakerOrder(Order order) {
        redisTemplate.opsForList().rightPush(Constant.Common.TMP_CANCEL_TAKER_ORDERS_KEY(), order);
    }

    @Override
    public List<Order> getAndDeleteCancelTakerOrders() {
        Long len = redisTemplate.opsForList().size(Constant.Common.TMP_CANCEL_TAKER_ORDERS_KEY());
        if (len == null || len == 0)
            return new ArrayList<>();
        else {
            List<Order> res = redisTemplate.opsForList().range(Constant.Common.TMP_CANCEL_TAKER_ORDERS_KEY(),
                    0, len-1);
            redisTemplate.opsForList().trim(Constant.Common.TMP_CANCEL_TAKER_ORDERS_KEY(), len, -1);
            return res;
        }
    }

}
