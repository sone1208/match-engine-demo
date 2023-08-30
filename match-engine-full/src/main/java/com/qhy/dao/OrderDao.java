package com.qhy.dao;

import com.qhy.pojo.Order;
import org.springframework.stereotype.Repository;

public interface OrderDao {
    public void setKey(String order_id, Order order);
    public Order getValue(String order_id);
    public void deleteKey(String order_id);

    public void setMaxOrderId(String id);
    public String getMaxOrderId();
}
