package com.qhy.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.BigDecimalTypeHandler;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成交记录（面向用户）
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@TableName(value = "TradingRecord", autoResultMap = true)
public class TradingRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("userid")
    private Integer userid;
    @TableField("stkcode")
    private String stkcode;
    @TableField(value = "price", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal price;
    @TableField(value = "qty", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal qty;
    @TableField("bsflag")
    private Boolean bsflag;
    @TableField("makerid")
    private Integer makerid;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone="GMT+8")
    @TableField(value = "tradingdate")
    private Date tradingdate;
    @DateTimeFormat(pattern = "HH:mm:ss")
    @JsonFormat(pattern = "HH:mm:ss", timezone="GMT+8")
    @TableField(value = "tradingtime")
    private Date tradingtime;


    public TradingRecord(Integer userid, String stkcode,
                         BigDecimal price, BigDecimal qty,
                         Boolean bsflag, Integer makerid,
                         Date tradingdate, Date tradingtime) {
        this.userid = userid;
        this.stkcode = stkcode;
        this.price = price;
        this.qty = qty;
        this.bsflag = bsflag;
        this.makerid = makerid;
        this.tradingdate = tradingdate;
        this.tradingtime = tradingtime;
    }
}
