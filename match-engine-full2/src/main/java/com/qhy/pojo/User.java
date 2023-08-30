package com.qhy.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "User", autoResultMap = true)
public class User {

    @TableId("userid")
    private Integer userid;
    @TableField("username")
    private String username;
    // Todo 删除
    @TableField(value = "orderids", typeHandler = FastjsonTypeHandler.class)
    private List<String> orderids;

    public User(Integer userid) {
        this.userid = userid;
    }
}
