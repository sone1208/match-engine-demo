package com.qhy.pojo;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;

import java.util.List;
import java.util.Set;

@TableName(value = "User", autoResultMap = true)
public class User {

    @TableId
    private Integer userId;
    @TableField("user_name")
    private String userName;
    @TableField(value = "share_ids", typeHandler = FastjsonTypeHandler.class)
    private Set<String> shareIds;
    @TableField(value = "order_ids", typeHandler = FastjsonTypeHandler.class)
    private Set<String> orderIds;

    public User() {
    }

    public User(Integer user_id) {
        this.userId = user_id;
    }

    public User(Integer user_id, String user_name, Set<String> share_ids, Set<String> order_ids) {
        this.userId = user_id;
        this.userName = user_name;
        this.shareIds = share_ids;
        this.orderIds = order_ids;
    }

    public void setUser_id(Integer user_id) {
        this.userId = user_id;
    }

    public void setUser_name(String user_name) {
        this.userName = user_name;
    }

    public void setShare_ids(Set<String> share_ids) {
        this.shareIds = share_ids;
    }

    public void setOrder_ids(Set<String> order_ids) {
        this.orderIds = order_ids;
    }

    public Integer getUser_id() {
        return userId;
    }

    public String getUser_name() {
        return userName;
    }

    public Set<String> getShare_ids() {
        return shareIds;
    }

    public Set<String> getOrder_ids() {
        return orderIds;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", shareIds=" + shareIds +
                ", orderIds=" + orderIds +
                '}';
    }
}
