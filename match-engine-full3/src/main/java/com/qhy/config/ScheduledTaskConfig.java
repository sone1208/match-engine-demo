package com.qhy.config;

import com.qhy.mapper.StockMapper;
import com.qhy.pojo.Stock;
import com.qhy.util.MyRunnable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.List;


@Slf4j
@Configuration
public class ScheduledTaskConfig implements SchedulingConfigurer {

    @Autowired
    private StockMapper stockMapper;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        //在注册器添加定时任务前添加线程池
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(16);
        taskScheduler.initialize();
        taskRegistrar.setTaskScheduler(taskScheduler);

        List<Stock> stocks = getStkCodeList();

        for (Stock stock : stocks) {
            String stkCode = stock.getStkcode();

            MyRunnable matchTask = new MyRunnable(
                    "MatchService",
                    "matchExecutor",
                    stkCode);
            MyRunnable matchBroadcastTask = new MyRunnable(
                    "MatchService",
                    "sendChangeOrders",
                    stkCode);

            log.info("添加了股票代码为" + stkCode + "的撮合定时任务和广播后台数据变化的定时任务");
            taskRegistrar.addFixedDelayTask(matchTask, 3000);
            taskRegistrar.addFixedRateTask(matchBroadcastTask, 3000);
            log.info("添加股票代码为" + stkCode + "的定时任务成功");
        }

        MyRunnable orderBroadcastTask = new MyRunnable(
                "OrderService",
                "sendChangeOrders");

        log.info("添加订单信息变化广播的定时任务");
        taskRegistrar.addFixedRateTask(orderBroadcastTask, 3000);
        log.info("添加订单信息变化广播的定时任务成功");
    }

    private List<Stock> getStkCodeList() {
        return stockMapper.selectList(null);
    }
}
