package com.qhy.dao;

import com.qhy.pojo.Order;

import java.math.BigDecimal;

public interface ShareDao {
    public void setNewMarketPrice(String share_id, BigDecimal new_market_price);
    public BigDecimal getMarketPrice(String share_id);
}
