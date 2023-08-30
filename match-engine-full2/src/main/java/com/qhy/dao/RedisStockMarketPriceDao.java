package com.qhy.dao;

import java.math.BigDecimal;

public interface RedisStockMarketPriceDao {
    public void setNewMarketPrice(String code, BigDecimal newMarketPrice);
    public BigDecimal getMarketPrice(String code);
}
