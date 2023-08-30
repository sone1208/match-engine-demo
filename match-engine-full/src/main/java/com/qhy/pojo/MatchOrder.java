package com.qhy.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import org.apache.ibatis.type.BigDecimalTypeHandler;

import java.math.BigDecimal;

@TableName(value = "MatchOrder", autoResultMap = true)
public class MatchOrder {
    @TableField(value = "share_id")
    public String shareId;
    @TableField(value = "price", typeHandler = BigDecimalTypeHandler.class)
    public BigDecimal price;
    @TableField(value = "amount", typeHandler = BigDecimalTypeHandler.class)
    public BigDecimal amount;
    @TableField(value = "taker_id")
    public Integer takerId;
    @TableField(value = "maker_id")
    public Integer makerId;

    public MatchOrder() {
    }

    public MatchOrder(String shareId, BigDecimal price, BigDecimal amount, Integer takerId, Integer makerId) {
        this.shareId = shareId;
        this.price = price;
        this.amount = amount;
        this.takerId = takerId;
        this.makerId = makerId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public Integer getTakerId() {
        return takerId;
    }

    public void setTakerId(Integer takerId) {
        this.takerId = takerId;
    }

    public Integer getMakerId() {
        return makerId;
    }

    public void setMakerId(Integer makerId) {
        this.makerId = makerId;
    }
}
