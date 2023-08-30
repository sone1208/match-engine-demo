package com.qhy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qhy.dao.MatchOrderDao;
import com.qhy.dao.UserDao;
import com.qhy.pojo.Direction;
import com.qhy.pojo.MatchOrder;
import com.qhy.pojo.Order;
import com.qhy.pojo.User;
import com.qhy.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;


@SpringBootTest
public class SimpleTests {

    @Autowired
    UserDao userDao;
    @Autowired
    MatchOrderDao matchOrderDao;

    @Autowired
    OrderService orderService;

    @Test
    public void testMatchRecord() {
        matchOrderDao.insert(new MatchOrder("60000", new BigDecimal("10.22"), new BigDecimal(1000), 1, 2));

    }

    @Test
    public void testUpdateById() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", 1);
        User user = userDao.selectOne(queryWrapper);

        Set<String> order_ids = user.getOrder_ids();
        order_ids.remove("0");
        user.setOrder_ids(order_ids);
        userDao.updateById(user);
    }

    @Test
    public void initUsers() {
        User user1 = new User(1, "A", null, null);
        User user2 = new User(2, "B", null, null);
        User user3 = new User(3, "C", null, null);
        User user4 = new User(4, "D", null, null);
        User user5 = new User(5, "E", null, null);

        userDao.insert(user1);
        userDao.insert(user2);
        userDao.insert(user3);
        userDao.insert(user4);
        userDao.insert(user5);
    }

    @Test
    public void testService() {

        User user1 = new User(1, "A", null, null);
        User user2 = new User(2, "B", null, null);
        User user3 = new User(3, "C", null, null);
        User user4 = new User(4, "D", null, null);
        User user5 = new User(5, "E", null, null);

        userDao.insert(user1);
        userDao.insert(user2);
        userDao.insert(user3);
        userDao.insert(user4);
        userDao.insert(user5);

        Order order1 = new Order(1, "60000", Direction.BUY,
                        new BigDecimal(10.20), new BigDecimal(1000));
        Order order2 = new Order(2, "60000", Direction.SELL,
                new BigDecimal(10.22), new BigDecimal(500));
        Order order3 = new Order(3, "60000", Direction.SELL,
                new BigDecimal(10.19), new BigDecimal(500));
        Order order4 = new Order(4, "60000", Direction.BUY,
                new BigDecimal(10.19), new BigDecimal(300));
        Order order5 = new Order(5, "60000", Direction.BUY,
                new BigDecimal(10.30), new BigDecimal(300));

        orderService.addOrder(order1);
        orderService.addOrder(order2);
        orderService.addOrder(order3);
        orderService.addOrder(order4);
        orderService.addOrder(order5);

        orderService.cancelOrder("0");
    }

    @Test
    public void testService2() {

        User user = new User(1, "A", null, null);
        userDao.insert(user);

        Order order = new Order(1, "60000", Direction.BUY,
                new BigDecimal(10.20), new BigDecimal(1000));
        orderService.addOrder(order);
    }

    @Test
    public void testService3() {
        System.out.println(orderService.getTakerOrders(2));
        System.out.println(orderService.getSubscribedOrders(2));
    }
}
