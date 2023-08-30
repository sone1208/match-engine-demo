package com.qhy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qhy.pojo.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
