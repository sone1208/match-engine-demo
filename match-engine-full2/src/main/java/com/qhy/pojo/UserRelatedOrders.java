package com.qhy.pojo;

import com.qhy.pojo.Order;
import com.qhy.pojo.Stock;
import com.qhy.pojo.TradingRecord;

import java.util.List;

public class UserRelatedOrders {

    public List<Order> takerOrders;
    public List<Order> subscribedOrders;
    public List<TradingRecord> tradingRecords;
    public List<Stock> stocks;
}
