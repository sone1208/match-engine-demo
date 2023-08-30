package com.qhy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qhy.util.WebSocket;
import com.qhy.dao.*;
import com.qhy.mapper.OrderMapper;
import com.qhy.mapper.TradingRecordMapper;
import com.qhy.mapper.UserMapper;
import com.qhy.pojo.Order;
import com.qhy.pojo.TradingRecord;
import com.qhy.pojo.User;
import com.qhy.service.MatchService;
import com.qhy.service.OrderService;
import com.qhy.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private WebSocket webSocket;
    @Lazy
    @Autowired
    private MatchService matchService;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
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

    @Override
    public List<Order> getTakerOrders(Integer userId) {
        // TODO 全量更新导致性能下降3倍，建议用增量更新
        log.info("后端向数据库获取用户" + userId + "的委托id清单");
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        User user = userMapper.selectOne(queryWrapper);

        log.info("后端根据用户" + userId + "的委托id清单向redis缓存获取order对象");
        List<Order> takerOrders = new ArrayList<>();
        if (user.getOrderids() == null)
            return takerOrders;
        for (String Orderid : user.getOrderids()) {
            takerOrders.add(redisOrderDao.getOrder(Orderid));
        }

        log.info("后端根据用户" + userId + "的委托id清单向redis缓存拿到对象，准备返回");
        return takerOrders;
    }

    @Override
    public List<Order> getSubscribedOrders(String code) {

        List<Order> buyAndSellOrders = new ArrayList<>();

        log.info("后端向卖盘中拿出至多前5个订单信息，股票代码为" + code);
        //先处理卖盘,取至多5个再倒序
        Set<String> sellOrders = redisOrderSellBookDao.getTopOrders(code, Constant.Common.SHOWED_ORDER_NUMBER());
        for (String order_id : sellOrders) {
            buyAndSellOrders.add(redisOrderDao.getOrder(order_id));
        }
        Collections.reverse(buyAndSellOrders);

        log.info("后端向买盘中拿出至多前5个订单信息，股票代码为" + code);
        //再处理买盘，取至多5个
        Set<String> buyOrders = redisOrderBuyBookDao.getTopOrders(code, Constant.Common.SHOWED_ORDER_NUMBER());
        for (String order_id : buyOrders) {
            buyAndSellOrders.add(redisOrderDao.getOrder(order_id));
        }

        log.info("后端返回组装好的行情表，且为5档行情，股票代码为" + code);
        return buyAndSellOrders;
    }

    @Override
    public List<TradingRecord> getTradingOrder(Integer userId) {
        // TODO 可能会涉及到全量更新问题的点，tradingRecord是一定不断累积的，不用每次查全表
        log.info("后端向数据库获取用户id为" + userId + "的成交记录");
        QueryWrapper<TradingRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        return tradingRecordMapper.selectList(queryWrapper);
    }

    @Override
    public void addOrder(Order order) {
        log.info("添加order订单前配置order信息");
        order = redisOrderDao.handleMaxOrderId(order);
        order.setQty(order.getOriginqty());
        order.setStatus(1);
        log.info("新订单id为"+order.getOrderid());

        //持久化
        //维护user信息
        log.info("维护用户表，将order的id" + order.getOrderid() + "添加到用户" + order.getUserid() + "的委托清单里");
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", order.getUserid());
        User user = userMapper.selectOne(queryWrapper);

        List<String> order_ids = user.getOrderids();
        if (order_ids == null)
            order_ids = new ArrayList<>();
        order_ids.add(order.getOrderid());
        user.setOrderids(order_ids);

        userMapper.updateById(user);

        //维护order信息
        log.info("维护order的数据库表，orderid为" + order.getOrderid());
        orderMapper.insert(order);



        //维护redis的缓存信息
        //维护order缓存表
        log.info("维护redis中的order映射表，orderid为" + order.getOrderid());
        redisOrderDao.setOrder(order.getOrderid(), order);

        //维护buy表和sell表，并检测展示的行情表是否加入了新的order，若是则通过websocket广播
        log.info("维护redis中的买盘和卖盘，orderid为" + order.getOrderid());
        Set<String> top5Orders;
        if (order.getBsflag()) {
            redisOrderBuyBookDao.addOrder(order.getStkcode(), order);
            top5Orders = redisOrderBuyBookDao.getTopOrders(order.getStkcode(),
                    Constant.Common.SHOWED_ORDER_NUMBER());
        } else {
            redisOrderSellBookDao.addOrder(order.getStkcode(), order);
            top5Orders = redisOrderSellBookDao.getTopOrders(order.getStkcode(),
                    Constant.Common.SHOWED_ORDER_NUMBER());
        }

        // Todo 定时推送
        if (top5Orders.contains(order.getOrderid())) {
            log.info("发现5档行情发生了更新，广播给订阅了股票的session，股票代码为" + order.getStkcode());
            //调用websocket方法广播消息，向订阅用户发送新的行情表
            try {
                webSocket.sendNewSubscribedOrders(getSubscribedOrders(order.getStkcode()), order.getStkcode());
            } catch (IOException e) {
                log.error("行情广播失败，股票id为" + order.getStkcode());
                e.printStackTrace();
            }
        }


        // Todo 设置成自动定时任务
        log.info("根据当前股票" + order.getStkcode() + "的处理状态决定是否开启新线程进行撮合");

        //并发操作尽量用双重校验，防止频繁加锁
        //判断当前股票盘的处理状态，未处理则交由线程池处理
        if (stockExecutingStatus.get(order.getStkcode()) == null) {
            stockExecutingStatus.putIfAbsent(order.getStkcode(), false);
        }

//        ReentrantLock thisLock;
//        if (!stockExecutingLock.containsKey(order.getStkcode())) {
//            thisLock = stockExecutingLock.computeIfAbsent(order.getStkcode(),
//                    key->new ReentrantLock(true));
//        } else {
//            thisLock = stockExecutingLock.get(order.getStkcode());
//        }

        //第一个加锁的位置，防止一支股票占用多个线程进行撮合
        if (!stockExecutingStatus.get(order.getStkcode())) {
            stockExecutingStatus.compute(order.getStkcode(), (key, value) -> {
                if (!value) {
                    log.info("判断通过开启新线程，股票代码为" + key);
                    matchService.matchExecutor(key);
                } else {
                    log.info("开启新线程判断未通过，不予开启，股票代码为" + key);
                }
                return true;
            });
        }

//        thisLock.lock();
//        try {
//            if (stockExecutingStatus.get(order.getStkcode()) == null ||
//                    !stockExecutingStatus.get(order.getStkcode())) {
//                log.info("判断通过开启新线程，股票代码为" + order.getStkcode());
//                stockExecutingStatus.put(order.getStkcode(), true);
//                thisLock.unlock();
//                matchService.matchExecutor(order.getStkcode());
//            } else {
//                log.info("开启新线程判断未通过，不予开启，股票代码为" + order.getStkcode());
//            }
//        } catch (Exception e) {
//            log.error("开启新线程判断加锁过程中出错");
//            e.printStackTrace();
//        } finally {
//            if (thisLock.isLocked())
//                thisLock.unlock();
//        }
    }

    @Override
    public boolean cancelOrder(String orderid) {
        log.info("准备对订单" + orderid + "进行撤单");
        Order order = redisOrderDao.getOrder(orderid);

        /**
         *  上锁防止调整状态的时候，match线程的状态检测被通过了
         *  设置为状态3以后，不会有其他操作能够处理当前order了
         */
        synchronized (order) {
            //除去已委托和部分交易，其他情况都看做不能撤回的状态
            if (order.getStatus() != 1 && order.getStatus() != 4) {
                log.warn("订单" + order.getOrderid() + "的状态为编号" + order.getStatus() + "不能撤单");
                return false;
            }

            log.info("订单" + orderid + "撤单检测通过，设置状态为正在撤单，并广播给对应用户");
            order.setStatus(3);
            //更新order缓存表
            redisOrderDao.setOrder(orderid, order);

            //发送带有正在删除状态的新表
            try {
                webSocket.sendNewTakerOrders(getTakerOrders(order.getUserid()), order.getUserid());
//                Thread.sleep(5000);
            } catch (IOException e) {
                throw new RuntimeException(e);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
            }
        }


        log.info("正式对订单" + orderid + "开始撤单");
        //正式开始撤单
        order.setStatus(6);
        //更新order数据库表
        orderMapper.updateById(order);

        log.info("检测对" + orderid + "撤单后5档行情是否变化，变化则对订阅股票" + order.getStkcode() + "的用户广播");
        //从买卖盘删除，但在删除以前先要检查top5买卖盘里是否有待撤单的订单，若有则广播
        Set<String> top5Orders;
        if (order.getBsflag()) {
            top5Orders = redisOrderBuyBookDao.getTopOrders(order.getStkcode(),
                    Constant.Common.SHOWED_ORDER_NUMBER());
            redisOrderBuyBookDao.deleteOrder(order.getStkcode(), order);
        } else {
            top5Orders = redisOrderSellBookDao.getTopOrders(order.getStkcode(),
                    Constant.Common.SHOWED_ORDER_NUMBER());
            redisOrderSellBookDao.deleteOrder(order.getStkcode(), order);
        }
        //发现行情表因撤单操作更改了
        if (top5Orders.contains(order.getOrderid())) {
            log.info("订单" + orderid + "撤单后5档行情发生了变化，对订阅股票" + order.getStkcode() + "的用户开始广播");
            //调用websocket方法广播消息，向订阅用户发送新的行情表
            try {
                webSocket.sendNewSubscribedOrders(getSubscribedOrders(order.getStkcode()), order.getStkcode());
            } catch (IOException e) {
                log.error("订单" + orderid + "撤单后5档行情发生了变化，对订阅股票"
                        + order.getStkcode() + "的用户广播过程中出错");
                e.printStackTrace();
            }
        }

        //更新redis中的表
        log.info("撤销订单" + orderid + "后更新redis表，并向对应用户" + order.getUserid() + "广播");
        redisOrderDao.setOrder(orderid, order);
        //广播变动的订单消息
        try {
            webSocket.sendNewTakerOrders(getTakerOrders(order.getUserid()), order.getUserid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

}
