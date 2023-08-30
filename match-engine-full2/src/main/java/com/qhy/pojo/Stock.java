package com.qhy.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.BigDecimalTypeHandler;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "Stock", autoResultMap = true)
public class Stock {

    @TableId("stkcode")
    private String stkcode;
    @TableField("stkname")
    private String stkname;

    /**
     * 开盘价；收盘价；最高价；最低价
     */
    @TableField("open")
    private BigDecimal open;
    @TableField("close")
    private BigDecimal close;
    @TableField("high")
    private BigDecimal high;
    @TableField("low")
    private BigDecimal low;

    /**
     * 总成交金额和总成交数量
     */
    @TableField(value = "amount", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal amount;
    @TableField(value = "vol", typeHandler = BigDecimalTypeHandler.class)
    private BigDecimal vol;

}
