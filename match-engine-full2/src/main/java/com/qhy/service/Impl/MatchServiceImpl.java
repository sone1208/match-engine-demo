package com.qhy.service.Impl;

import com.qhy.util.WebSocket;
import com.qhy.dao.*;
import com.qhy.mapper.MatchRecordMapper;
import com.qhy.mapper.OrderMapper;
import com.qhy.mapper.StockMapper;
import com.qhy.mapper.TradingRecordMapper;
import com.qhy.pojo.MatchRecord;
import com.qhy.pojo.Order;
import com.qhy.pojo.Stock;
import com.qhy.pojo.TradingRecord;
import com.qhy.service.MatchService;
import com.qhy.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.swing.plaf.basic.BasicButtonUI;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class MatchServiceImpl implements MatchService {

    @Autowired
    private WebSocket webSocket;
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private MatchRecordMapper matchRecordMapper;
    @Autowired
    private TradingRecordMapper tradingRecordMapper;

    @Autowired
    @Qualifier("RedisOrderDao")
    private RedisOrderDao redisOrderDao;
    @Autowired
    @Qualifier("RedisOrderBuyBookDao")
    private RedisBSOrderDao redisOrderBuyBookDao;
    @Autowired
    @Qualifier("RedisOrderSellBookDao")
    private RedisBSOrderDao redisOrderSellBookDao;
    @Autowired
    @Qualifier("RedisStockMarketPriceDao")
    private RedisStockMarketPriceDao redisStockMarketPriceDao;

    @Override
    @Async("matchThreadPool")
    public void matchExecutor(String code) {

        // Todo 做成定时任务
        log.info("对股票" + code + "开始撮合");
        while(true) {
            log.info("开始校验股票"+code);
            /**
             * 取两个book的top订单
             */
            String topSellOrderId = redisOrderSellBookDao.getTopOneOrder(code);
            String topBuyOrderId = redisOrderBuyBookDao.getTopOneOrder(code);

            /**
             * 两个校验决定是否退出循环：1.买盘或卖盘为空；2.价格原因无法再撮合
             */
            log.info("对股票" + code + "买卖盘是否为空进行校验");
            //双重校验，第一层校验初步判断，防止每轮撮合都要加锁
            if (topSellOrderId == null || topBuyOrderId == null) {
                log.info("对股票" + code + "买卖盘是否为空第一层校验通过");
                //第二个加锁的位置，将第二层校验和更改股票处理状态的操作锁在同一流程，防止中间插入新订单的线程申请，而遗漏新撮合的处理
                boolean res = OrderService.stockExecutingStatus.compute(code, (key, value) -> {
                   if ((redisOrderSellBookDao.getTopOneOrder(key) == null) ||
                           (redisOrderBuyBookDao.getTopOneOrder(key) == null)) {
                       log.info("对股票" + key + "买卖盘是否为空第二层校验通过，撮合退出");
                       return false;
                   } else {
                       return true;
                   }
                });

                if (!res) return ;
                else continue ;

//                ReentrantLock thisLock = OrderService.stockExecutingLock.get(code);
//                thisLock.lock();
//                try {
//                    //第二层校验判定orderBook是否发生变化，防止第一层校验以后出现新的变化
//                    if ((redisOrderSellBookDao.getTopOneOrder(code) == null) ||
//                            (redisOrderBuyBookDao.getTopOneOrder(code) == null)) {
//                        log.info("对股票" + code + "买卖盘是否为空第二层校验通过，撮合退出");
//                        OrderService.stockExecutingStatus.put(code, false);
//                        return ;
//                    } else {
//                        continue;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    thisLock.unlock();
//                }
            }

            Order topSellOrder = redisOrderDao.getOrder(topSellOrderId);
            Order topBuyOrder = redisOrderDao.getOrder(topBuyOrderId);

            log.info("对股票" + code + "买卖盘价格是否可以撮合进行校验");
            //另一个双重校验，逻辑相同，判定相比稍复杂
            if (topSellOrder.getPrice().compareTo(topBuyOrder.getPrice()) > 0) {
                log.info("对股票" + code + "买卖盘价格是否可以撮合第一层校验通过");

                boolean res = OrderService.stockExecutingStatus.compute(code, (key, value) -> {
                    if (redisOrderDao.getOrder(redisOrderSellBookDao.getTopOneOrder(key)).getPrice()
                       .compareTo
                       (redisOrderDao.getOrder(redisOrderBuyBookDao.getTopOneOrder(key)).getPrice()) > 0) {
                        log.info("对股票" + key + "买卖盘价格是否可以撮合第二层校验通过，撮合退出");
                        return false;
                    } else {
                        return true;
                    }
                });

                if (!res) return ;
                else continue ;

//                ReentrantLock thisLock = OrderService.stockExecutingLock.get(code);
//                thisLock.lock();
//                try {
//                    //第二层校验是为了判断top1订单是否发生变化，没变化则校验成功，否则再进行一轮循环
//                    if (topSellOrder.getOrderid().equals(redisOrderSellBookDao.getTopOneOrder(code)) &&
//                            topBuyOrder.getOrderid().equals(redisOrderBuyBookDao.getTopOneOrder(code))) {
//                        log.info("对股票" + code + "买卖盘价格是否可以撮合第二层校验通过，撮合退出");
//                        OrderService.stockExecutingStatus.put(code, false);
//                        return ;
//                    } else {
//                        continue;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    thisLock.unlock();
//                }
            }

            /**
             * 先校验状态并更改状态，再进行之后的处理
             */
            log.info("检测买单"+topBuyOrderId+"和卖单"+topSellOrderId+"的状态，如果正在删除则暂不撮合");
            if(!tryChangeStatusToMatch(topBuyOrder, topSellOrder))
                continue;


            log.info("所有校验通过，开始对股票" + code + "开始撮合");
            /**
             * 校验通过开始撮合
             */
            log.info("股票" + code + "撮合：确定成交量");
            // 成交数量为两者较小值:
            BigDecimal matchedVol = topSellOrder.getQty().min(topBuyOrder.getQty());

            log.info("股票" + code + "撮合：确定成交价");
            //成交价格根据市场价、买入价、卖出价决定，并更新市场价
            BigDecimal marketPrice = redisStockMarketPriceDao.getMarketPrice(code);
            //简化方法，将较早订单的价格作为市场价，实际情况需要竞价
            if (marketPrice.equals(new BigDecimal(-1))) {
                if (topSellOrderId.length() > topBuyOrderId.length()) {
                    marketPrice = topBuyOrder.getPrice();
                } else if (topSellOrderId.length() < topBuyOrderId.length()) {
                    marketPrice = topSellOrder.getPrice();
                } else {
                    if (topSellOrderId.compareTo(topBuyOrderId) < 0) {
                        marketPrice = topSellOrder.getPrice();
                    } else {
                        marketPrice = topBuyOrder.getPrice();
                    }
                }
            }
            BigDecimal matchedPrice = getMatchPrice(marketPrice, topBuyOrder.getPrice(), topSellOrder.getPrice());
            log.info("股票" + code + "撮合：确定成交价后更新市场价");
            //更新市场价
            redisStockMarketPriceDao.setNewMarketPrice(code, matchedPrice);

            // Todo 批量插入
            log.info("股票" + code + "撮合完成，开始存储记录");
            /**
             * 撮合完成，存储记录，包括redis更新，mysql持久化
             * 涉及表：撮合记录MatchRecord，成交记录TradingRecord，股票信息stock，订单信息order表，redis中的买卖盘
             */
            log.info("股票" + code + "撮合完成，开始存储记录，当前记录撮合记录");
            //存储撮合记录（面向交易所）
            MatchRecord newMatchRecord = new MatchRecord(code, matchedPrice, matchedVol,
                    topBuyOrder.getUserid(), topSellOrder.getUserid());
            matchRecordMapper.insert(newMatchRecord);

            log.info("股票" + code + "撮合完成，开始存储记录，当前记录成交记录");
            //存储成交记录（面向用户）
            TradingRecord buyerTradingRecord = new TradingRecord(topBuyOrder.getUserid(), code,
                    matchedPrice, matchedVol,
                    true, topSellOrder.getUserid());
            TradingRecord sellerTradingRecord = new TradingRecord(topSellOrder.getUserid(), code,
                    matchedPrice, matchedVol,
                    false, topBuyOrder.getUserid());
            tradingRecordMapper.insert(buyerTradingRecord);
            tradingRecordMapper.insert(sellerTradingRecord);

            log.info("股票" + code + "撮合完成，开始存储记录，当前记录股票记录");
            // Todo 删除股票
            //更新stock记录
            Stock stock = stockMapper.selectById(code);
            if (stock.getOpen() == null) {
                stock.setOpen(matchedPrice);
                stock.setHigh(matchedPrice);
                stock.setLow(matchedPrice);
                stock.setAmount(matchedPrice.multiply(matchedVol));
                stock.setVol(matchedVol);
            } else {
                if (stock.getLow().compareTo(matchedPrice) > 0) {
                    stock.setLow(matchedPrice);
                }
                if (stock.getHigh().compareTo(matchedPrice) < 0) {
                    stock.setHigh(matchedPrice);
                }

                stock.setAmount(stock.getAmount().add(matchedPrice.multiply(matchedVol)));
                stock.setVol(stock.getVol().add(matchedVol));
            }
            stockMapper.updateById(stock);

            //更新order的剩余数量
            topSellOrder.setQty(topSellOrder.getQty().subtract(matchedVol));
            topBuyOrder.setQty(topBuyOrder.getQty().subtract(matchedVol));

            log.info("股票" + code + "撮合完成，开始存储记录，当前更新行情表记录");
            //将两个top中完全成交的从orderBook删除，并更新订单状态
            if (topSellOrder.getQty().signum() == 0) {
                redisOrderSellBookDao.deleteOrder(code, topSellOrder);
                topSellOrder.setStatus(5);
            } else {
                topSellOrder.setStatus(4);
            }
            if (topBuyOrder.getQty().signum() == 0) {
                redisOrderBuyBookDao.deleteOrder(code, topBuyOrder);
                topBuyOrder.setStatus(5);
            } else {
                topBuyOrder.setStatus(4);
            }

            log.info("股票" + code + "撮合完成，开始存储记录，当前向mysql存储order信息");
            //更新成交后的订单数量，并存到mysql和redis中
            orderMapper.updateById(topBuyOrder);
            orderMapper.updateById(topSellOrder);

            log.info("股票" + code + "撮合完成，开始存储记录，当前向redis存储order信息");
            redisOrderDao.setOrder(topBuyOrderId, topBuyOrder);
            redisOrderDao.setOrder(topSellOrderId, topSellOrder);

            /**
             * 撮合完成，将新行情、新订单表、新成交记录推送给对应用户
             */
            log.info("股票" + code + "撮合完成，存储记录完成，广播新的信息");
            try {
                webSocket.sendNewSubscribedOrders(orderService.getSubscribedOrders(code), code);
                webSocket.sendNewTakerOrders(orderService.getTakerOrders(topSellOrder.getUserid()),
                        topSellOrder.getUserid());
                webSocket.sendNewTakerOrders(orderService.getTakerOrders(topBuyOrder.getUserid()),
                        topBuyOrder.getUserid());
                webSocket.sendNewTradingRecords(orderService.getTradingOrder(topSellOrder.getUserid()),
                        topSellOrder.getUserid());
                webSocket.sendNewTradingRecords(orderService.getTradingOrder(topBuyOrder.getUserid()),
                        topBuyOrder.getUserid());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean tryChangeStatusToMatch(Order buyOrder, Order sellOrder) {
        /**
         * 同时锁住两个对象进行操作，防止一个已经修改了状态而另一个状态不符合要求的情况
         */
        synchronized (buyOrder) {
            synchronized (sellOrder) {
                //除去已委托和部分交易，其他情况都看做不能交易的状态，可能的情况只有正在撤回
                if (buyOrder.getStatus() != 1 && buyOrder.getStatus() != 4) {
                    log.warn("订单" + buyOrder.getOrderid() + "状态为" + buyOrder.getStatus() + "，暂停0.5s再继续撮合");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return false;
                }
                if (sellOrder.getStatus() != 1 && sellOrder.getStatus() != 4) {
                    log.warn("订单" + sellOrder.getOrderid() + "状态为" + sellOrder.getStatus() + "，暂停0.5s再继续撮合");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return false;
                }

                log.info("即将撮合的两个订单" + buyOrder.getOrderid() + "和" + sellOrder.getOrderid() + "状态符合，可撮合" +
                        "向对应用户广播状态为正在交易的新委托订单表");
                //更新order缓存表
                buyOrder.setStatus(2);
                redisOrderDao.setOrder(buyOrder.getOrderid(), buyOrder);
                sellOrder.setStatus(2);
                redisOrderDao.setOrder(sellOrder.getOrderid(), sellOrder);

                //发送带有正在交易状态的新表
                try {
                    webSocket.sendNewTakerOrders(orderService.getTakerOrders(buyOrder.getUserid()),
                            buyOrder.getUserid());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    webSocket.sendNewTakerOrders(orderService.getTakerOrders(sellOrder.getUserid()),
                            sellOrder.getUserid());
//                    Thread.sleep(5000);
                } catch (IOException e) {
                    throw new RuntimeException(e);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    private BigDecimal getMatchPrice(BigDecimal marketPrice, BigDecimal buyerPrice, BigDecimal sellerPrice) {
        if (marketPrice.compareTo(buyerPrice) != -1) {
            return buyerPrice;
        } else if (sellerPrice.compareTo(marketPrice) != -1) {
            return sellerPrice;
        } else {
            return marketPrice;
        }
    }


}
