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

}
