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

        // Todo 是否应该测试好撮合能力以后，再确定每3s中最多撮合的次数

        while(true) {
            log.info("开始校验股票"+code);
            /**
             * 取两个book的top订单
             */
            Integer topSellOrderId = redisOrderSellBookDao.getTopOneOrder(code);
            Integer topBuyOrderId = redisOrderBuyBookDao.getTopOneOrder(code);

            /**
             * 两个校验决定是否退出循环：1.买盘或卖盘为空；2.价格原因无法再撮合
             */
            log.info("对股票" + code + "买卖盘是否为空进行校验");
            if (topSellOrderId == null || topBuyOrderId == null) {
                log.info("对股票" + code + "买卖盘是否为空校验后发现出现了空盘，准备退出");
                return;
            }

            Order topSellOrder = redisOrderDao.getOrder(topSellOrderId);
            Order topBuyOrder = redisOrderDao.getOrder(topBuyOrderId);

            log.info("对股票" + code + "买卖盘价格是否可以撮合进行校验");
            if (topSellOrder.getPrice().compareTo(topBuyOrder.getPrice()) > 0) {
                log.info("对股票" + code + "买卖盘价格是否可以撮合校验后，发现此时价格已不能再撮合，准备退出");
                return;
            }

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

            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前向redis存储order信息");
            //更新发生成交的订单到redis中
            redisOrderDao.putOrder(topBuyOrderId, topBuyOrder);
            redisOrderDao.putOrder(topSellOrderId, topSellOrder);

            /**
             * 除去新的买卖盘和redis中的order信息需要更新外，其余部分采用批量更新的方式
             * 行情表是较独立模块，可单独更新
             */
            log.info("股票" + code +
                    "撮合id" + topSellOrderId + "和id" + topBuyOrderId + "：完成，开始存储记录，当前记录在mysql存储的order");
            //预存储发生成交的订单
            redisChangeInfo.addTakerOrder(code, topBuyOrder);
            redisChangeInfo.addTakerOrder(code, topSellOrder);

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
        }
    }

    @Override
    public void sendChangeOrders(String code) {

        log.info("广播股票" + code + "的新行情快照");
        try {
            webSocket.sendNewSubscribedOrders(getSubscribedOrders(code), code);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<MatchRecord> matchRecords = redisChangeInfo.getMatchRecords(code);
        List<TradingRecord> tradingRecords = redisChangeInfo.getTradingRecords(code);
        List<Order> takerOrders = redisChangeInfo.getTakerOrders(code);

        int len = matchRecords.size();
        if (len == 0)
            return ;

        //首先进行批量插入和更新
        log.info("批量插入" + len + "条撮合记录");
        matchRecordMapper.insertBatchSomeColumn(matchRecords.subList(0, len));
        log.info("批量插入" + 2 * len + "条成交记录");
        tradingRecordMapper.insertBatchSomeColumn(tradingRecords.subList(0, 2 * len));
        log.info("批量更新" + 2 * len + "条订单信息记录");
        orderMapper.updateBatch(takerOrders.subList(0, 2 * len));

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

        log.info("将处理过的临时记录删除");
        redisChangeInfo.deleteHandledMatchRecords(code, len);
        redisChangeInfo.deleteHandledTradingRecord(code, 2 * len);
        redisChangeInfo.deleteHandledTakerOrders(code, 2 * len);
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
