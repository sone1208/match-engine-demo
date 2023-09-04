package com.qhy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;


public interface MyBaseMapper <T> extends BaseMapper<T> {

    int insertBatchSomeColumn(List<T> entityList);
    int updateBatch(List<T> entityList);
}
