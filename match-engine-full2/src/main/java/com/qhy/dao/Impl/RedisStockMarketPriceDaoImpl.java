package com.qhy.dao.Impl;

import com.qhy.dao.RedisStockMarketPriceDao;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository("RedisStockMarketPriceDao")
public class RedisStockMarketPriceDaoImpl implements RedisStockMarketPriceDao {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void setNewMarketPrice(String code, BigDecimal newMarketPrice) {
        redisTemplate.opsForHash().put(Constant.Common.STOCK_MARKET_PRICE(), code, newMarketPrice);
    }

    @Override
    public BigDecimal getMarketPrice(String code) {
        if (!redisTemplate.opsForHash().hasKey(Constant.Common.STOCK_MARKET_PRICE(), code))
            return new BigDecimal(-1);
        return (BigDecimal) redisTemplate.opsForHash().get(Constant.Common.STOCK_MARKET_PRICE(), code);
    }
}
