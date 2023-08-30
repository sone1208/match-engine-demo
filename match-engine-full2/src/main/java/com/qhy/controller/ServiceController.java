package com.qhy.controller;

import com.qhy.pojo.Order;
import com.qhy.pojo.Results;
import com.qhy.pojo.UserRelatedOrders;
import com.qhy.service.OrderService;
import com.qhy.service.StockService;
import com.qhy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/users")
public class ServiceController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockService stockService;

    @GetMapping
    public Results getUsers() {
        log.info("前端向后端获取用户列表");
        return new Results(userService.getUsers());
    }

    @GetMapping("/{id}")
    public Results getUserOrders(@PathVariable Integer id) {
        log.info("前端向后端获取某个用户相关的订单消息，包括持有委托，股票库，成交记录");
        UserRelatedOrders userRelatedOrders = new UserRelatedOrders();
        userRelatedOrders.takerOrders = orderService.getTakerOrders(id);
        userRelatedOrders.tradingRecords = orderService.getTradingOrder(id);
        userRelatedOrders.stocks = stockService.getStocks();
        return new Results(userRelatedOrders);
    }

    @GetMapping("/{id}/{code}")
    public Results getSubscribedOrders(@PathVariable(value = "id") Integer userId,
                                           @PathVariable(value = "code") String code) {
        log.info("前端id为" + userId + "的用户为向后端获取股票id为" + code + "的行情表");
        return new Results(orderService.getSubscribedOrders(code));
    }

    @PostMapping("/{id}")
    public Results addNewOrders(@PathVariable(value = "id") Integer id,
                                    @RequestBody Order order) {
        log.info("id为" + id + "的用户请求添加新订单");
        order.setUserid(id);
        orderService.addOrder(order);

        UserRelatedOrders userRelatedOrders = new UserRelatedOrders();
        userRelatedOrders.takerOrders = orderService.getTakerOrders(id);
        userRelatedOrders.subscribedOrders = orderService.getSubscribedOrders(order.getStkcode());
        log.info("id为" + id + "的用户请求添加新订单成功，向前端返回新的委托表和订阅行情表");
        return new Results(userRelatedOrders);
    }

    @DeleteMapping("/{id}/{orderId}")
    public Results deleteOrders(@PathVariable(value = "id") Integer id,
                                   @PathVariable(value = "orderId") String orderId) {
        log.info("id为" + id + "的用户请求删除订单" + orderId);
        boolean success =  orderService.cancelOrder(orderId);
        if (!success) {
            log.error("id为" + id + "的用户请求删除订单" + orderId + "失败，返回空结果");
            return new Results();
        } else {
            UserRelatedOrders userRelatedOrders = new UserRelatedOrders();
            userRelatedOrders.takerOrders = orderService.getTakerOrders(id);
//            userRelatedOrders.subscribedOrders = orderService.getSubscribedOrders(order.getCode());
            log.info("id为" + id + "的用户请求删除订单" + orderId + "操作成功返回新的订单委托表");
            return new Results(userRelatedOrders);
        }
    }

}
