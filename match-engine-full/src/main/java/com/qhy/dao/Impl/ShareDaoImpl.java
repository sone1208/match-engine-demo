package com.qhy.dao.Impl;

import com.qhy.dao.ShareDao;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository("ShareDaoImpl")
public class ShareDaoImpl implements ShareDao {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void setNewMarketPrice(String share_id, BigDecimal new_market_price) {
        redisTemplate.opsForHash().put(Constant.Common.SHARE_PRICE(), share_id, new_market_price);
    }

    @Override
    public BigDecimal getMarketPrice(String share_id) {
        if (!redisTemplate.opsForHash().hasKey(Constant.Common.SHARE_PRICE(), share_id))
            return new BigDecimal(-1);
        return (BigDecimal) redisTemplate.opsForHash().get(Constant.Common.SHARE_PRICE(), share_id);
    }
}
