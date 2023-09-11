package com.qhy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qhy.dao.RedisBSOrderDao;
import com.qhy.dao.RedisChangeInfo;
import com.qhy.dao.RedisOrderDao;
import com.qhy.mapper.OrderMapper;
import com.qhy.mapper.TradingRecordMapper;
import com.qhy.pojo.Order;
import com.qhy.pojo.TradingRecord;
import com.qhy.pojo.UserRelatedInfo;
import com.qhy.service.OrderService;
import com.qhy.util.WebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Slf4j
@Service("OrderService")
public class OrderServiceImpl implements OrderService {

    @Autowired
    private WebSocket webSocket;
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
    @Autowired
    @Qualifier("RedisChangeInfo")
    private RedisChangeInfo redisChangeInfo;

    @Override
    public UserRelatedInfo getUserRelatedOrders(Integer userId) {
        log.info("获取用户id" + userId + "相关的订单信息");
        UserRelatedInfo userRelatedInfo = new UserRelatedInfo();

        log.info("获取用户id" + userId + "的委托订单");
        QueryWrapper<Order> queryWrapperOrder = new QueryWrapper<>();
        queryWrapperOrder.eq("userid", userId);
        userRelatedInfo.takerOrders = orderMapper.selectList(queryWrapperOrder);
        log.info("获取用户id" + userId + "的委托订单完成");


        log.info("获取用户id" + userId + "的成交订单");
        QueryWrapper<TradingRecord> queryWrapperTradingRecord = new QueryWrapper<>();
        queryWrapperTradingRecord.eq("userid", userId);
        userRelatedInfo.tradingRecords = tradingRecordMapper.selectList(queryWrapperTradingRecord);
        log.info("获取用户id" + userId + "的成交订单完成");

        return userRelatedInfo;
    }

    @Override
    public Order addOrder(Order order) {

        // Todo 添加订单的执行时间太长，待优化

        long stime = System.nanoTime();
        log.info("添加order订单前配置order信息");
        order.setQty(order.getOriginqty());
        order.setStatus(1);

        //将order信息首先插入到数据库，同时可生成id信息
        long stime1 = System.nanoTime();
        log.info("维护order的数据库表，orderid为" + order.getOrderid());
        orderMapper.insert(order);
        long etime1 = System.nanoTime();

        //维护redis的缓存信息
        //维护order缓存表
        log.info("维护redis中的order映射表，orderid为" + order.getOrderid());
        redisOrderDao.putOrder(order.getOrderid(), order);

        //维护buy表和sell表
        log.info("维护redis中的买盘和卖盘，orderid为" + order.getOrderid());
        if (order.getBsflag()) {
            redisOrderBuyBookDao.addOrder(order.getStkcode(), order);
        } else {
            redisOrderSellBookDao.addOrder(order.getStkcode(), order);
        }

        //暂存新order
        log.info("暂存新的order订单" + order.getOrderid());
        redisChangeInfo.addNewTakerOrder(order);

        log.info("添加order订单" + order.getOrderid() + "执行结束，返回新新生成的订单");
        long etime = System.nanoTime();
        log.warn("本轮订单添加执行时长：" + ((etime - stime) / 1000000.0) + " 毫秒. ");
//        log.warn("本轮mysql落库时间比例：" + (((double)(etime1 - stime1)) / (etime - stime)));
        return order;
    }

    @Override
    public boolean cancelOrder(Integer orderid) {
        log.info("准备对订单" + orderid + "进行撤单");
        Order order = redisOrderDao.getOrder(orderid);

        log.info("对订单" + orderid + "redis中的状态进行更新");
        // 对订单在redis中的状态进行更新，lua保证原子操作
        log.info("执行lua脚本将订单状态改为3：正在撤单");
        if (!redisOrderDao.changeOneOrderStatusByLua(order, 3)) {
            log.info("订单状态修改失败，直接退出");
            return false;
        }

        //正式开始撤单
        log.info("正式对订单" + orderid + "开始撤单");
        order.setStatus(6);
        //更新order数据库表
        log.info("对订单" + orderid + "数据库中的状态进行更新");
        orderMapper.updateById(order);

        //从买卖盘删除
        log.info("将订单" + orderid + "从买卖盘中进行删除");
        if (order.getBsflag()) {
            redisOrderBuyBookDao.deleteOrder(order.getStkcode(), order);
        } else {
            redisOrderSellBookDao.deleteOrder(order.getStkcode(), order);
        }

        //更新redis中的表
        log.info("撤销订单" + orderid + "后更新redis表，并向对应用户" + order.getUserid() + "广播");
        redisOrderDao.putOrder(orderid, order);

        //暂存撤销order
        log.info("暂存撤销的order订单" + order.getOrderid());
        redisChangeInfo.addCancelTakerOrder(order);

        return true;
    }

    @Override
    public void sendChangeOrders() {
        log.info("向用户的其他session连接广播新增订单和撤销订单");
        List<Order> newTakerOrders = redisChangeInfo.getAndDeleteNewTakerOrders();
        List<Order> cancelTakerOrders = redisChangeInfo.getAndDeleteCancelTakerOrders();

        // Todo 通过userId组织起来统一发送 与 单条信息一一发送相比是否更高效

        log.info("处理新增订单");
        HashMap<Integer, List<Order>> newTakerOrdersMap = new HashMap<>();
        log.info("遍历一遍记录，将新增的order跟用户id映射绑定");
        for (int i = 0; i < newTakerOrders.size(); i++) {
            List<Order> newTakerOrdersForUser = newTakerOrdersMap.get(newTakerOrders.get(i).getUserid());
            if (newTakerOrdersForUser == null) {
                newTakerOrdersForUser = new ArrayList<>();
            }

            newTakerOrdersForUser.add(newTakerOrders.get(i));
            newTakerOrdersMap.put(newTakerOrders.get(i).getUserid(), newTakerOrdersForUser);
        }

        log.info("开始广播新的order信息");
        try {
            Set<Integer> newRelatedUsers = newTakerOrdersMap.keySet();
            for (Integer userId : newRelatedUsers) {
                webSocket.sendNewTakerOrders(newTakerOrdersMap.get(userId), userId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        log.info("处理撤销的订单");
        HashMap<Integer, List<Order>> cancelTakerOrdersMap = new HashMap<>();
        log.info("遍历一遍记录，将撤销的order跟用户id映射绑定");
        for (int i = 0; i < cancelTakerOrders.size(); i++) {
            List<Order> cancelTakerOrdersForUser = cancelTakerOrdersMap.get(cancelTakerOrders.get(i).getUserid());
            if (cancelTakerOrdersForUser == null) {
                cancelTakerOrdersForUser = new ArrayList<>();
            }

            cancelTakerOrdersForUser.add(cancelTakerOrders.get(i));
            cancelTakerOrdersMap.put(cancelTakerOrders.get(i).getUserid(), cancelTakerOrdersForUser);
        }

        log.info("开始广播撤销的order信息");
        try {
            Set<Integer> cancelRelatedUsers = cancelTakerOrdersMap.keySet();
            for (Integer userId : cancelRelatedUsers) {
                webSocket.sendChangeTakerOrders(cancelTakerOrdersMap.get(userId), userId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
