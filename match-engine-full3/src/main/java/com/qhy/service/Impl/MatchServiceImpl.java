package com.qhy.service.Impl;

import com.qhy.dao.RedisBSOrderDao;
import com.qhy.dao.RedisChangeInfo;
import com.qhy.dao.RedisOrderDao;
import com.qhy.dao.RedisStockMarketPriceDao;
import com.qhy.mapper.MatchRecordMapper;
import com.qhy.mapper.OrderMapper;
import com.qhy.mapper.TradingRecordMapper;
import com.qhy.pojo.MatchRecord;
import com.qhy.pojo.Order;
import com.qhy.pojo.TradingRecord;
import com.qhy.service.MatchService;
import com.qhy.util.Constant;
import com.qhy.util.WebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service("MatchService")
public class MatchServiceImpl implements MatchService {

    @Autowired
    private WebSocket webSocket;
    @Autowired
    private OrderMapper orderMapper;
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
    @Qualifier("RedisChangeInfo")
    private RedisChangeInfo redisChangeInfo;
    @Autowired
    @Qualifier("RedisStockMarketPriceDao")
    private RedisStockMarketPriceDao redisStockMarketPriceDao;

    @Override
    public List<Order> getSubscribedOrders(String code) {
        List<Order> buyAndSellOrders = new ArrayList<>();

        log.info("后端向卖盘中拿出至多前5个订单信息，股票代码为" + code);
        //先处理卖盘,取至多5个再倒序
        Set<Integer> sellOrders = redisOrderSellBookDao.getTopOrders(code, Constant.Common.SHOWED_ORDER_NUMBER());
        for (Integer order_id : sellOrders) {
            buyAndSellOrders.add(redisOrderDao.getOrder(order_id));
        }
        Collections.reverse(buyAndSellOrders);

        log.info("后端向买盘中拿出至多前5个订单信息，股票代码为" + code);
        //再处理买盘，取至多5个
        Set<Integer> buyOrders = redisOrderBuyBookDao.getTopOrders(code, Constant.Common.SHOWED_ORDER_NUMBER());
        for (Integer order_id : buyOrders) {
            buyAndSellOrders.add(redisOrderDao.getOrder(order_id));
        }

        log.info("后端返回组装好的行情表，且为5档行情，股票代码为" + code);
        return buyAndSellOrders;
    }

    @Override
    public void matchExecutor(String code) {
        log.info("对股票" + code + "开始撮合");

        while(true) {
            long stime = System.nanoTime();
            log.info("开始校验股票"+code);
            /**
             * 取两个book的top订单
             */
            long stime1 = System.nanoTime();
            Integer topSellOrderId = redisOrderSellBookDao.getTopOneOrder(code);
            Integer topBuyOrderId = redisOrderBuyBookDao.getTopOneOrder(code);
            long etime1 = System.nanoTime();

            /**
             * 两个校验决定是否退出循环：1.买盘或卖盘为空；2.价格原因无法再撮合
             */
            log.info("对股票" + code + "买卖盘是否为空进行校验");
            if (topSellOrderId == null || topBuyOrderId == null) {
                log.info("对股票" + code + "买卖盘是否为空校验后发现出现了空盘，准备退出");
                break;
            }

            long stime2 = System.nanoTime();
            Order topSellOrder = redisOrderDao.getOrder(topSellOrderId);
            Order topBuyOrder = redisOrderDao.getOrder(topBuyOrderId);
            long etime2 = System.nanoTime();

            log.info("对股票" + code + "买卖盘价格是否可以撮合进行校验");
            if (topSellOrder.getPrice().compareTo(topBuyOrder.getPrice()) > 0) {
                log.info("对股票" + code + "买卖盘价格是否可以撮合校验后，发现此时价格已不能再撮合，准备退出");
                break;
            }

            long stime3 = System.nanoTime();
            log.info("执行lua脚本将订单状态改为2：正在交易");
            if (!redisOrderDao.changeTwoOrderStatusByLua(topSellOrder, topBuyOrder)) {
                log.info("订单状态修改失败，sleep 0.1s 再继续撮合");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            long etime3 = System.nanoTime();

            log.info("所有校验通过，开始对股票" + code +
                    "盘中id" + topSellOrderId + "和id" + topBuyOrderId + "开始撮合");
            /**
             * 校验通过开始撮合
             */
            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：确定成交量");
            // 成交数量为两者较小值:
            BigDecimal matchedVol = topSellOrder.getQty().min(topBuyOrder.getQty());

            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：确定成交价");
            //成交价格根据市场价、买入价、卖出价决定，并更新市场价
            BigDecimal marketPrice = redisStockMarketPriceDao.getMarketPrice(code);
            //简化方法，将较早订单的价格作为市场价，实际情况需要竞价
            if (marketPrice.equals(new BigDecimal(-1))) {
                if (topSellOrderId < topBuyOrderId) {
                    marketPrice = topSellOrder.getPrice();
                } else {
                    marketPrice = topBuyOrder.getPrice();
                }
            }
            BigDecimal matchedPrice = getMatchPrice(marketPrice, topBuyOrder.getPrice(), topSellOrder.getPrice());

            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：确定成交价后更新市场价");
            //更新市场价
            redisStockMarketPriceDao.setNewMarketPrice(code, matchedPrice);

            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录");
            /**
             * 撮合完成，存储记录：包括redis更新，mysql持久化
             * 涉及表：撮合记录MatchRecord，成交记录TradingRecord，订单信息order表，redis中的买卖盘
             */
            //更新order的剩余数量
            topSellOrder.setQty(topSellOrder.getQty().subtract(matchedVol));
            topBuyOrder.setQty(topBuyOrder.getQty().subtract(matchedVol));

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前更新行情表记录");
            //将两个top中完全成交的从orderBook删除，并更新订单状态
            long stime4 = System.nanoTime();
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
            long etime4 = System.nanoTime();

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前向redis存储order信息");
            //更新发生成交的订单到redis中
            long stime5 = System.currentTimeMillis();
            redisOrderDao.putOrder(topBuyOrderId, topBuyOrder);
            redisOrderDao.putOrder(topSellOrderId, topSellOrder);
            long etime5 = System.currentTimeMillis();

            /**
             * 除去新的买卖盘和redis中的order信息需要更新外，其余部分采用批量更新的方式
             * 行情表是较独立模块，可单独更新
             */
            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前记录在mysql存储的order");
            //预存储发生成交的订单
            long stime6 = System.nanoTime();
            redisChangeInfo.addTakerOrder(code, topBuyOrder);
            redisChangeInfo.addTakerOrder(code, topSellOrder);
            long etime6 = System.nanoTime();

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前记录成交记录");
            //预存储成交记录（面向用户）
            TradingRecord buyerTradingRecord = new TradingRecord(topBuyOrder.getUserid(), code,
                    matchedPrice, matchedVol,
                    true, topSellOrder.getUserid());
            TradingRecord sellerTradingRecord = new TradingRecord(topSellOrder.getUserid(), code,
                    matchedPrice, matchedVol,
                    false, topBuyOrder.getUserid());
            redisChangeInfo.addTradingRecord(code, buyerTradingRecord);
            redisChangeInfo.addTradingRecord(code, sellerTradingRecord);

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前记录撮合记录");
            //预存储撮合记录（面向交易所）
            //撮合记录最后记录，在持久化时，只需要看撮合记录的数量，就能决定前两个数据的数量了
            MatchRecord newMatchRecord = new MatchRecord(code, matchedPrice, matchedVol,
                    topBuyOrder.getUserid(), topSellOrder.getUserid());
            redisChangeInfo.addMatchRecord(code, newMatchRecord);

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：本轮全部完成");

            long etime = System.nanoTime();
//            log.warn("本轮撮合执行时长：" + ((etime - stime) / 1000000.0) + " 毫秒. ");
//            log.warn("本轮撮合取首订单时间比例：" + (((double)(etime1 - stime1)) / (etime - stime)));
//            log.warn("本轮撮合根据id获取订单信息时间比例：" + (((double)(etime2 - stime2)) / (etime - stime)));
//            log.warn("本轮撮合执行lua脚本时间比例：" + (((double)(etime3 - stime3)) / (etime - stime)));
//            log.warn("本轮撮合从跳表删记录时间比例：" + (((double)(etime4 - stime4)) / (etime - stime)));
//            log.warn("本轮撮合更新订单信息时间比例：" + (((double)(etime5 - stime5)) / (etime - stime)));
//            log.warn("本轮撮合订单变化临时信息存储时间比例：" + (((double)(etime6 - stime6)) / (etime - stime)));
        }
    }

    @Override
    public void matchExecutorMuti(String code) {
        log.info("对股票" + code + "开始批量撮合");

        while(true) {
            long stime = System.nanoTime();
            log.info("开始校验股票"+code);
            /**
             * 取两个book的top订单
             */
            long stime1 = System.nanoTime();
            Integer topSellOrderId = redisOrderSellBookDao.getTopOneOrder(code);
            Integer topBuyOrderId = redisOrderBuyBookDao.getTopOneOrder(code);
            long etime1 = System.nanoTime();

            /**
             * 两个校验决定是否退出循环：1.买盘或卖盘为空；2.价格原因无法再撮合
             */
            log.info("对股票" + code + "买卖盘是否为空进行校验");
            if (topSellOrderId == null || topBuyOrderId == null) {
                log.info("对股票" + code + "买卖盘是否为空校验后发现出现了空盘，准备退出");
                break;
            }

            long stime2 = System.nanoTime();
            Order topSellOrder = redisOrderDao.getOrder(topSellOrderId);
            Order topBuyOrder = redisOrderDao.getOrder(topBuyOrderId);
            long etime2 = System.nanoTime();

            log.info("对股票" + code + "买卖盘价格是否可以撮合进行校验");
            if (topSellOrder.getPrice().compareTo(topBuyOrder.getPrice()) > 0) {
                log.info("对股票" + code + "买卖盘价格是否可以撮合校验后，发现此时价格已不能再撮合，准备退出");
                break;
            }

            /**
             * 获取价格优于当前取到订单价格的所有订单
             */
            log.info("对股票" + code + "获得当前取到的最优订单价格的所有订单");
            List<Integer> topSellOrderIds = redisOrderSellBookDao.getTopOrdersByScore(code, topSellOrder);
            List<Integer> topBuyOrderIds = redisOrderBuyBookDao.getTopOrdersByScore(code, topBuyOrder);

            //需要额外校验一次，防止买卖盘变化了，出现空结果
            if (topBuyOrderIds == null || topSellOrderIds == null) {
                log.info("对股票" + code + "获得当前取到的最优订单价格的所有订单失败，订单可能已撤单");
                break;
            }

            long stime3 = System.nanoTime();
            log.info("根据orderId的列表批量获取order");
            List<Order> topSellOrders = redisOrderDao.getOrders(topSellOrderIds);
            List<Order> topBuyOrders = redisOrderDao.getOrders(topBuyOrderIds);

            log.info("循环列表执行lua脚本将订单状态改为2：正在交易，将状态修改失败的（正常情况是删除了）去掉，同时记录总数量");
            BigDecimal totalSellQTY = new BigDecimal(0);
            BigDecimal totalBuyQTY = new BigDecimal(0);

            Iterator<Order> iterator = topSellOrders.listIterator();
            while(iterator.hasNext()) {
                Order handlingOrder = iterator.next();
                if (!redisOrderDao.changeOneOrderStatusByLua(handlingOrder, 2)) {
                    iterator.remove();
                } else {
                    totalSellQTY.add(handlingOrder.getQty());
                }
            }

            iterator = topBuyOrders.listIterator();
            while(iterator.hasNext()) {
                Order handlingOrder = iterator.next();
                if (!redisOrderDao.changeOneOrderStatusByLua(handlingOrder, 2)) {
                    iterator.remove();
                } else {
                    totalBuyQTY.add(handlingOrder.getQty());
                }
            }
            long etime3 = System.nanoTime();





            log.info("所有校验通过，开始对股票" + code +
                    "盘中id" + topSellOrderId + "和id" + topBuyOrderId + "及其价格相同的买卖单开始撮合");
            /**
             * 校验通过开始撮合
             */
            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：确定成交价");
            //成交价格根据市场价、买入价、卖出价决定，并更新市场价
            BigDecimal marketPrice = redisStockMarketPriceDao.getMarketPrice(code);
            //简化方法，将较早订单的价格作为市场价，实际情况需要竞价
            if (marketPrice.equals(new BigDecimal(-1))) {
                if (topSellOrderId < topBuyOrderId) {
                    marketPrice = topSellOrder.getPrice();
                } else {
                    marketPrice = topBuyOrder.getPrice();
                }
            }
            BigDecimal matchedPrice = getMatchPrice(marketPrice, topBuyOrder.getPrice(), topSellOrder.getPrice());

            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：确定成交价后更新市场价");
            //更新市场价
            redisStockMarketPriceDao.setNewMarketPrice(code, matchedPrice);




            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "及其价格相同的买卖单：确定成交量");
            BigDecimal matchedVol = topSellOrder.getQty().min(topBuyOrder.getQty());

            //双指针遍历，确定成交量的同时存储记录
            log.info("股票" + code + "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录");
            /**
             * 撮合完成，存储记录：包括redis更新，mysql持久化
             * 涉及表：撮合记录MatchRecord，成交记录TradingRecord，订单信息order表，redis中的买卖盘
             */

            //更新order的剩余数量
            topSellOrder.setQty(topSellOrder.getQty().subtract(matchedVol));
            topBuyOrder.setQty(topBuyOrder.getQty().subtract(matchedVol));

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前更新行情表记录");
            //将两个top中完全成交的从orderBook删除，并更新订单状态
            long stime4 = System.nanoTime();
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
            long etime4 = System.nanoTime();

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前向redis存储order信息");
            //更新发生成交的订单到redis中
            long stime5 = System.currentTimeMillis();
            redisOrderDao.putOrder(topBuyOrderId, topBuyOrder);
            redisOrderDao.putOrder(topSellOrderId, topSellOrder);
            long etime5 = System.currentTimeMillis();

            /**
             * 除去新的买卖盘和redis中的order信息需要更新外，其余部分采用批量更新的方式
             * 行情表是较独立模块，可单独更新
             */
            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前记录在mysql存储的order");
            //预存储发生成交的订单
            long stime6 = System.nanoTime();
            redisChangeInfo.addTakerOrder(code, topBuyOrder);
            redisChangeInfo.addTakerOrder(code, topSellOrder);
            long etime6 = System.nanoTime();

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前记录成交记录");
            //预存储成交记录（面向用户）
            TradingRecord buyerTradingRecord = new TradingRecord(topBuyOrder.getUserid(), code,
                    matchedPrice, matchedVol,
                    true, topSellOrder.getUserid());
            TradingRecord sellerTradingRecord = new TradingRecord(topSellOrder.getUserid(), code,
                    matchedPrice, matchedVol,
                    false, topBuyOrder.getUserid());
            redisChangeInfo.addTradingRecord(code, buyerTradingRecord);
            redisChangeInfo.addTradingRecord(code, sellerTradingRecord);

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前记录撮合记录");
            //预存储撮合记录（面向交易所）
            //撮合记录最后记录，在持久化时，只需要看撮合记录的数量，就能决定前两个数据的数量了
            MatchRecord newMatchRecord = new MatchRecord(code, matchedPrice, matchedVol,
                    topBuyOrder.getUserid(), topSellOrder.getUserid());
            redisChangeInfo.addMatchRecord(code, newMatchRecord);

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：本轮全部完成");

            long etime = System.nanoTime();
//            log.warn("本轮撮合执行时长：" + ((etime - stime) / 1000000.0) + " 毫秒. ");
//            log.warn("本轮撮合取首订单时间比例：" + (((double)(etime1 - stime1)) / (etime - stime)));
//            log.warn("本轮撮合根据id获取订单信息时间比例：" + (((double)(etime2 - stime2)) / (etime - stime)));
//            log.warn("本轮撮合执行lua脚本时间比例：" + (((double)(etime3 - stime3)) / (etime - stime)));
//            log.warn("本轮撮合从跳表删记录时间比例：" + (((double)(etime4 - stime4)) / (etime - stime)));
//            log.warn("本轮撮合更新订单信息时间比例：" + (((double)(etime5 - stime5)) / (etime - stime)));
//            log.warn("本轮撮合订单变化临时信息存储时间比例：" + (((double)(etime6 - stime6)) / (etime - stime)));
        }
    }

    @Override
    public void sendChangeOrders(String code) {

        long stime = System.currentTimeMillis();

        log.info("广播股票" + code + "的新行情快照");
        try {
            webSocket.sendNewSubscribedOrders(getSubscribedOrders(code), code);
        } catch (IOException e) {

            // Todo 具体处理

            throw new RuntimeException(e);
        }

        List<MatchRecord> matchRecords = redisChangeInfo.getAndDeleteMatchRecords(code);
        int len = matchRecords.size();
        if (len == 0)
            return ;

        List<TradingRecord> tradingRecords = redisChangeInfo.getAndDeleteTradingRecords(code, 2*(long)len);
        List<Order> takerOrders = redisChangeInfo.getAndDeleteTakerOrders(code, 2*(long)len);

        //首先进行批量插入和更新
        log.info("批量插入" + len + "条撮合记录");
        matchRecordMapper.insertBatchSomeColumn(matchRecords);
        log.info("批量插入" + 2 * len + "条成交记录");
        tradingRecordMapper.insertBatchSomeColumn(tradingRecords);
        log.info("批量更新" + 2 * len + "条订单信息记录");
        orderMapper.updateBatch(takerOrders);

        // Todo 与单条信息一一发送相比是否更高效

        HashMap<Integer, List<Order>> takerOrdersForUser = new HashMap<>();
        HashMap<Integer, List<TradingRecord>> tradingRecordsForUser = new HashMap<>();

        log.info("遍历一遍记录，将变化的order和新生成的成交记录跟用户id映射绑定");
        for (int i = 0; i < 2 * len; i++) {
            List<Order> newTakerOrders = takerOrdersForUser.get(takerOrders.get(i).getUserid());
            List<TradingRecord> newTradingRecords = tradingRecordsForUser.get(tradingRecords.get(i).getUserid());
            if (newTakerOrders == null) {
                newTakerOrders = new ArrayList<>();
            }
            if (newTradingRecords == null) {
                newTradingRecords = new ArrayList<>();
            }

            // Todo 订单信息的改变是否可以优化为只看最后一次的结果

            newTakerOrders.add(takerOrders.get(i));
            takerOrdersForUser.put(takerOrders.get(i).getUserid(), newTakerOrders);

            newTradingRecords.add(tradingRecords.get(i));
            tradingRecordsForUser.put(tradingRecords.get(i).getUserid(), newTradingRecords);
        }

        log.info("开始广播新的成交记录，变化的order信息");
        try {
            //变化的order信息涉及到的用户id集合必然是跟撮合记录的用户id集合完全吻合的
            Set<Integer> changeRelatedUsers = takerOrdersForUser.keySet();

            log.info("广播新的order信息和新的成交记录");
            for (Integer userId : changeRelatedUsers) {
                webSocket.sendChangeTakerOrders(takerOrdersForUser.get(userId), userId);
                webSocket.sendNewTradingRecords(tradingRecordsForUser.get(userId), userId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("广播结束");

        long etime = System.currentTimeMillis();
        if (len > 0) {
//            log.warn("本轮落库和广播执行时长：" + (etime - stime) + " 毫秒，涉及成交订单" + len + "条");
        }
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
