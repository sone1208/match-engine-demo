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

@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "OrderInfo")
public class Order {

    @TableId(value = "orderid", type = IdType.AUTO)
    private Integer orderid;
    @TableField("userid")
    private Integer userid;
    @TableField("stkcode")
    private String stkcode;
    @TableField("bsflag")
    private Boolean bsflag;
    @TableField(value = "price", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal price;
    @TableField(value = "originqty", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal originqty;
    @TableField(value = "qty", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal qty;

    /**
     * 1 已委托
     * 2 正在交易
     * 3 正在撤单
     * 4 部分交易
     * 5 完成交易
     * 6 已撤单
     */
    @TableField("status")
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone="GMT+8")
    @TableField(value = "orderdate", fill = FieldFill.INSERT)
    private Date orderdate;
    @DateTimeFormat(pattern = "HH:mm:ss")
    @JsonFormat(pattern = "HH:mm:ss", timezone="GMT+8")
    @TableField(value = "ordertime", fill = FieldFill.INSERT)
    private Date ordertime;
    @DateTimeFormat(pattern = "HH:mm:ss")
    @JsonFormat(pattern = "HH:mm:ss", timezone="GMT+8")
    @TableField(value = "canceltime", fill = FieldFill.UPDATE)
    private Date canceltime;

    public Order(Integer userid, String stkcode, Boolean bsflag,
                 BigDecimal price, BigDecimal originqty, BigDecimal qty,
                 Integer status) {
        this.userid = userid;
        this.stkcode = stkcode;
        this.bsflag = bsflag;
        this.price = price;
        this.originqty = originqty;
        this.qty = qty;
        this.status = status;
    }
}
