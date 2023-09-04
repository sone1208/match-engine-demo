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
        List<MatchRecord> matchRecords = getMatchRecords(code);
        matchRecords.add(matchRecord);
        redisTemplate.opsForHash().put(Constant.Common.TMP_MATCH_RECORDS_KEY(), code, matchRecords);
    }

    @Override
    public List<MatchRecord> getMatchRecords(String code) {
        List<MatchRecord> matchRecords;
        Object tmp_object = redisTemplate.opsForHash().get(Constant.Common.TMP_MATCH_RECORDS_KEY(), code);
        if (tmp_object == null) {
            matchRecords = new ArrayList<>();
        } else {
            matchRecords = (List<MatchRecord>) tmp_object;
        }
        return matchRecords;
    }

    @Override
    public void deleteHandledMatchRecords(String code, Integer len) {
        if (len == 0)
            return ;

        List<MatchRecord> matchRecords = getMatchRecords(code);
        if (len == matchRecords.size()) {
            redisTemplate.opsForHash().put(Constant.Common.TMP_MATCH_RECORDS_KEY(), code,
                    new ArrayList<MatchRecord>());
        } else {
            redisTemplate.opsForHash().put(Constant.Common.TMP_MATCH_RECORDS_KEY(), code,
                    new ArrayList<>(matchRecords.subList(len, matchRecords.size())));
        }
    }

    @Override
    public void addTradingRecord(String code, TradingRecord tradingRecord) {
        List<TradingRecord> tradingRecords = getTradingRecords(code);
        tradingRecords.add(tradingRecord);
        redisTemplate.opsForHash().put(Constant.Common.TMP_TRADING_RECORDS_KEY(), code, tradingRecords);
    }

    @Override
    public List<TradingRecord> getTradingRecords(String code) {
        List<TradingRecord> tradingRecords;
        Object tmp_object = redisTemplate.opsForHash().get(Constant.Common.TMP_TRADING_RECORDS_KEY(), code);
        if (tmp_object == null) {
            tradingRecords = new ArrayList<>();
        } else {
            tradingRecords = (List<TradingRecord>) tmp_object;
        }
        return tradingRecords;
    }

    @Override
    public void deleteHandledTradingRecord(String code, Integer len) {
        if (len == 0)
            return ;

        List<TradingRecord> tradingRecords = getTradingRecords(code);
        if (len == tradingRecords.size()) {
            redisTemplate.opsForHash().put(Constant.Common.TMP_TRADING_RECORDS_KEY(), code,
                    new ArrayList<TradingRecord>());
        } else {
            redisTemplate.opsForHash().put(Constant.Common.TMP_TRADING_RECORDS_KEY(), code,
                    new ArrayList<>(tradingRecords.subList(len, tradingRecords.size())));
        }
    }

    @Override
    public void addTakerOrder(String code, Order order) {
        List<Order> takerOrders = getTakerOrders(code);
        takerOrders.add(order);
        redisTemplate.opsForHash().put(Constant.Common.TMP_TAKER_ORDERS_KEY(), code, takerOrders);
    }

    @Override
    public List<Order> getTakerOrders(String code) {
        List<Order> takerOrders;
        Object tmp_object = redisTemplate.opsForHash().get(Constant.Common.TMP_TAKER_ORDERS_KEY(), code);
        if (tmp_object == null) {
            takerOrders = new ArrayList<>();
        } else {
            takerOrders = (List<Order>) tmp_object;
        }
        return takerOrders;
    }

    @Override
    public void deleteHandledTakerOrders(String code, Integer len) {
        if (len == 0)
            return ;

        List<Order> takerOrders = getTakerOrders(code);
        if (len == takerOrders.size()) {
            redisTemplate.opsForHash().put(Constant.Common.TMP_TAKER_ORDERS_KEY(), code,
                    new ArrayList<Order>());
        } else {
            redisTemplate.opsForHash().put(Constant.Common.TMP_TAKER_ORDERS_KEY(), code,
                    new ArrayList<>(takerOrders.subList(len, takerOrders.size())));
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
            for (int i = 0; i < len; i++) {
                redisTemplate.opsForList().leftPop(Constant.Common.TMP_NEW_TAKER_ORDERS_KEY());
            }
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
            for (int i = 0; i < len; i++) {
                redisTemplate.opsForList().leftPop(Constant.Common.TMP_CANCEL_TAKER_ORDERS_KEY());
            }
            return res;
        }
    }

}
