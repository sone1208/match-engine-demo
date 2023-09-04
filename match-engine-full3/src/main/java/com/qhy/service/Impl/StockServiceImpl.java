package com.qhy.service.Impl;

import com.qhy.mapper.StockMapper;
import com.qhy.pojo.Stock;
import com.qhy.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class StockServiceImpl implements StockService {
    @Autowired
    StockMapper stockMapper;

    @Override
    public List<Stock> getStocks() {
        log.info("获取当前股票池");
        return stockMapper.selectList(null);
    }
}
