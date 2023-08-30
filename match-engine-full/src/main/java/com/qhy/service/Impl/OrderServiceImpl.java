package com.qhy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qhy.dao.*;
import com.qhy.pojo.Direction;
import com.qhy.pojo.MatchOrder;
import com.qhy.pojo.Order;
import com.qhy.pojo.User;
import com.qhy.service.OrderService;
import com.qhy.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    UserDao userDao;

    @Autowired
    MatchOrderDao matchOrderDao;

    @Autowired
    @Qualifier("ShareDaoImpl")
    ShareDao shareDao;

    @Autowired
    @Qualifier("OrderDaoImpl")
    OrderDao orderDao;

    @Autowired
    @Qualifier("OrderBuyBookDaoImpl")
    OrderBookDao orderBuyBookDao;

    @Autowired
    @Qualifier("OrderSellBookDaoImpl")
    OrderBookDao orderSellBookDao;

    @Override
    public List<Order> getTakerOrders(Integer user_id) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user_id);
        User user = userDao.selectOne(queryWrapper);

        List<Order> takerOrders = new ArrayList<>();
        if (user.getOrder_ids() == null)
            return takerOrders;
        for (String order_id : user.getOrder_ids()) {
            takerOrders.add(orderDao.getValue(order_id));
        }

        return takerOrders;
    }

    @Override
    public List<String> getSubscribedShares(Integer user_id) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user_id);
        User user = userDao.selectOne(queryWrapper);

        List<String> subscribedShares = new ArrayList<>();
        if (user.getShare_ids() != null) {
            for (String share_id : user.getShare_ids()) {
                subscribedShares.add(share_id);
            }
        }

        return subscribedShares;
    }

    @Override
    public Map<String, List<Order>> getSubscribedOrders(Integer user_id) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user_id);
        User user = userDao.selectOne(queryWrapper);

        HashMap<String, List<Order>> subcribedOrders = new HashMap<>();
        if (user.getShare_ids() == null)
            return subcribedOrders;

        //根据用户的share_id分别向buyBook和sellBook取order对象
        for (String share_id : user.getShare_ids()) {
            List<Order> buyAndSellOrders = new ArrayList<>();

            //先处理卖盘,取至多5个再倒序
            Set<String> sellOrders = orderSellBookDao.getTopOrders(share_id, Constant.Common.SHOWED_ORDER_NUMBER());
            for (String order_id : sellOrders) {
                buyAndSellOrders.add(orderDao.getValue(order_id));
            }
            Collections.reverse(buyAndSellOrders);

            //再处理买盘，取至多5个
            Set<String> buyOrders = orderBuyBookDao.getTopOrders(share_id, Constant.Common.SHOWED_ORDER_NUMBER());
            for (String order_id : buyOrders) {
                buyAndSellOrders.add(orderDao.getValue(order_id));
            }

            if (buyAndSellOrders.size() != 0) {
                subcribedOrders.put(share_id, buyAndSellOrders);
            }
        }

        return subcribedOrders;
    }

    @Override
    public void addOrder(Order order) {
        order.setOrder_id(String.valueOf(Long.valueOf(orderDao.getMaxOrderId())+1L));
        orderDao.setMaxOrderId(order.getOrder_id());
        order.setAmount(order.getOrigin_amount());

        //进行撮合
        order = match(order);

        //维护委托信息
        orderDao.setKey(order.getOrder_id(), order);

        // Taker订单未完全成交时，放入对应买卖盘:
        if (order.getAmount().signum() > 0) {
            if (order.getDirection() == Direction.BUY) {
                orderBuyBookDao.addOrder(order.getShare_id(), order);
            } else {
                orderSellBookDao.addOrder(order.getShare_id(), order);
            }
        }

        //维护user信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", order.getUser_id());
        User user = userDao.selectOne(queryWrapper);

        Set<String> share_ids = user.getShare_ids();
        if (share_ids == null)
            share_ids = new HashSet<>();
        share_ids.add(order.getShare_id());
        user.setShare_ids(share_ids);

        Set<String> order_ids = user.getOrder_ids();
        if (order_ids == null)
            order_ids = new HashSet<>();
        order_ids.add(order.getOrder_id());
        user.setOrder_ids(order_ids);

        userDao.updateById(user);
    }

    @Override
    public boolean cancelOrder(String order_id) {
        Order order = orderDao.getValue(order_id);

        //更新用户数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", order.getUser_id());
        User user = userDao.selectOne(queryWrapper);

        Set<String> order_ids = user.getOrder_ids();
        order_ids.remove(order_id);
        user.setOrder_ids(order_ids);
        userDao.updateById(user);

        //从委托列表删除
        orderDao.deleteKey(order_id);

        //从买卖盘删除
        if (order.getAmount().signum() != 0) {
            if (order.getDirection() == Direction.BUY) {
                orderBuyBookDao.deleteOrder(order.getShare_id(), order);
            } else {
                orderSellBookDao.deleteOrder(order.getShare_id(), order);
            }
        }

        return true;
    }

    @Override
    public List<MatchOrder> getMatchOrder(Integer user_id) {
        QueryWrapper<MatchOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("taker_id", user_id).or().eq("maker_id", user_id);
        return matchOrderDao.selectList(queryWrapper);
    }



    private Order match(Order order) {

        //获取买卖盘的order list
        Set<String> sellIdBook = orderSellBookDao.getAllOrders(order.getShare_id());
        Set<String> buyIdBook = orderBuyBookDao.getAllOrders(order.getShare_id());

        List<Order> sellBook = new ArrayList<>();
        List<Order> buyBook = new ArrayList<>();
        for (String id : sellIdBook) {
            sellBook.add(orderDao.getValue(id));
        }
        for (String id : buyIdBook) {
            buyBook.add(orderDao.getValue(id));
        }

        //根据买卖方向处理
        if (order.getDirection() == Direction.BUY) {
            return match(order, sellBook, buyBook);
        } else {
            return match(order, buyBook, sellBook);
        }
    }

    private Order match(Order order, List<Order> makerBook, List<Order> anotherBook) {

        // makerBook为null时直接返回
        if (makerBook == null)
            return order;

        //依次遍历makerBook
        for (Order makerOrder : makerBook) {
            // 买入订单价格比卖盘第一档价格低:
            if (order.getDirection() == Direction.BUY && order.getPrice().compareTo(makerOrder.getPrice()) < 0) {
                break;
            }
            // 卖出订单价格比买盘第一档价格高:
            else if (order.getDirection() == Direction.SELL && order.getPrice().compareTo(makerOrder.getPrice()) > 0) {
                break;
            }

            // 成交数量为两者较小值:
            BigDecimal matchedAmount = order.getAmount().min(makerOrder.getAmount());
            //成交价格根据市场价、买入价、卖出价决定，并更新市场价
            BigDecimal market_price = shareDao.getMarketPrice(order.getShare_id());
            market_price = market_price.equals(new BigDecimal(-1))
                    ? makerOrder.getPrice() : market_price;

            BigDecimal match_price;
            if (order.getDirection() == Direction.BUY) {
                match_price = getMatchPrice(market_price, order.getPrice(), makerOrder.getPrice());
            } else {
                match_price = getMatchPrice(market_price, makerOrder.getPrice(), order.getPrice());
            }

            shareDao.setNewMarketPrice(order.getShare_id(), match_price);

            //存储成交记录
            matchOrderDao.insert(new MatchOrder(order.getShare_id(), match_price, matchedAmount,
                    order.getUser_id(), makerOrder.getUser_id()));

            // 更新成交后的订单数量，并存到redis中:
            order.setAmount(order.getAmount().subtract(matchedAmount));
            makerOrder.setAmount(makerOrder.getAmount().subtract(matchedAmount));

            orderDao.setKey(order.getOrder_id(), order);
            orderDao.setKey(makerOrder.getOrder_id(), makerOrder);

            // 对手盘完全成交后，从盘中删除:
            if (makerOrder.getAmount().signum() == 0) {
                if (makerOrder.getDirection() == Direction.BUY) {
                    orderBuyBookDao.deleteOrder(makerOrder.getShare_id(), makerOrder);
                } else {
                    orderSellBookDao.deleteOrder(makerOrder.getShare_id(), makerOrder);
                }
            }

            // Taker订单完全成交后，退出循环:
            if (order.getAmount().signum() == 0) {
                break;
            }
        }

        return order;
    }

    private BigDecimal getMatchPrice(BigDecimal market_price, BigDecimal buyer_price, BigDecimal seller_price) {
        if (market_price.compareTo(buyer_price) != -1) {
            return buyer_price;
        } else if (seller_price.compareTo(market_price) != -1) {
            return seller_price;
        } else {
            return market_price;
        }
    }

}
