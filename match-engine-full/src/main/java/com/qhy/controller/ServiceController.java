package com.qhy.controller;

import com.qhy.pojo.Direction;
import com.qhy.pojo.Order;
import com.qhy.pojo.User;
import com.qhy.service.OrderService;
import com.qhy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class ServiceController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    private UserOrders updateOrdersById(Integer id) {
        UserOrders userOrders = new UserOrders();
        userOrders.takerOrders = orderService.getTakerOrders(id);
        userOrders.subscribedShares = orderService.getSubscribedShares(id);
        userOrders.subscribedOrders = orderService.getSubscribedOrders(id);
        userOrders.matchOrders = orderService.getMatchOrder(id);

        return userOrders;
    }

    @GetMapping
    public List<User> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/{id}")
    public UserOrders getAllOrderById(@PathVariable Integer id) {
        return updateOrdersById(id);
    }

    @PostMapping("/{id}/{direction}")
    public UserOrders addNewOrders(@PathVariable(value = "id") Integer id,
                                   @PathVariable(value = "direction") Integer direction,
                                   @RequestBody Order order) {
        order.setUser_id(id);
        if (direction == 0)
            order.setDirection(Direction.BUY);
        else
            order.setDirection((Direction.SELL));

        orderService.addOrder(order);
        return updateOrdersById(id);
    }

    @DeleteMapping("/{id}/{order_id}")
    public UserOrders deleteOrders(@PathVariable(value = "id") Integer id,
                                   @PathVariable(value = "order_id") String order_id) {
        orderService.cancelOrder(order_id);
        return updateOrdersById(id);
    }

}
