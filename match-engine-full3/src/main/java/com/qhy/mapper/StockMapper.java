package com.qhy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qhy.pojo.Stock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {
}
