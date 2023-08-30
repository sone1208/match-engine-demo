package com.qhy.pojo;

import java.math.BigDecimal;

public class Order{
    private String order_id;
    private Integer user_id;
    private String share_id;

    private Direction direction;
    private BigDecimal price;
    private BigDecimal origin_amount;
    private BigDecimal amount;

    public Order() {
    }

    public Order(Integer user_id, String share_id, Direction direction, BigDecimal price, BigDecimal origin_amount) {
        this.user_id = user_id;
        this.share_id = share_id;
        this.direction = direction;
        this.price = price;
        this.origin_amount = origin_amount;
    }

    public Order(String order_id, Integer user_id, String share_id, Direction direction, BigDecimal price, BigDecimal origin_amount, BigDecimal amount) {
        this.order_id = order_id;
        this.user_id = user_id;
        this.share_id = share_id;
        this.direction = direction;
        this.price = price;
        this.origin_amount = origin_amount;
        this.amount = amount;
    }

    public String getOrder_id() {
        return order_id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public String getShare_id() {
        return share_id;
    }

    public Direction getDirection() {
        return direction;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOrigin_amount() {
        return origin_amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public void setShare_id(String share_id) {
        this.share_id = share_id;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setOrigin_amount(BigDecimal origin_amount) {
        this.origin_amount = origin_amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Order{" +
                "order_id=" + order_id +
                ", user_id=" + user_id +
                ", share_id=" + share_id +
                ", direction=" + direction +
                ", price=" + price +
                ", origin_amount=" + origin_amount +
                ", amount=" + amount +
                '}';
    }

}
