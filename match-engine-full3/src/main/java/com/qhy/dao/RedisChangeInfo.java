package com.qhy.dao;

import com.qhy.pojo.MatchRecord;
import com.qhy.pojo.Order;
import com.qhy.pojo.TradingRecord;

import java.util.List;

public interface RedisChangeInfo {
    public void addMatchRecord(String code, MatchRecord matchRecord);
    public List<MatchRecord> getMatchRecords(String code);
    public void deleteHandledMatchRecords(String code, Integer len);
    public void addTradingRecord(String code, TradingRecord tradingRecord);
    public List<TradingRecord> getTradingRecords(String code);
    public void deleteHandledTradingRecord(String code, Integer len);

    /**
     * TakerOrder：跟撮合完成相关的订单变化
     * NewTakerOrder：新加入的订单
     * CancelTakerOrder：撤销的订单
     */

    public void addTakerOrder(String code, Order order);
    public List<Order> getTakerOrders(String code);
    public void deleteHandledTakerOrders(String code, Integer len);
    public void addNewTakerOrder(Order order);
    public List<Order> getAndDeleteNewTakerOrders();
    public void addCancelTakerOrder(Order order);
    public List<Order> getAndDeleteCancelTakerOrders();
}
