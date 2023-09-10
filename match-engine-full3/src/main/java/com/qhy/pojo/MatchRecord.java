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
 * 撮合记录（面向交易所）
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "MatchRecord")
public class MatchRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("stkcode")
    private String stkcode;
    @TableField(value = "price", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal price;
    @TableField(value = "qty", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal qty;
    @TableField("buyerid")
    private Integer buyerid;
    @TableField("sellerid")
    private Integer sellerid;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone="GMT+8")
    @TableField(value = "matchdate")
    private Date matchdate;
    @DateTimeFormat(pattern = "HH:mm:ss")
    @JsonFormat(pattern = "HH:mm:ss", timezone="GMT+8")
    @TableField(value = "matchtime")
    private Date matchtime;


    public MatchRecord(String stkcode, BigDecimal price, BigDecimal qty,
                       Integer buyerid, Integer sellerid, Date matchdate, Date matchtime) {
        this.stkcode = stkcode;
        this.price = price;
        this.qty = qty;
        this.buyerid = buyerid;
        this.sellerid = sellerid;
        this.matchdate = matchdate;
        this.matchtime = matchtime;
    }
}
