package com.qhy.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 配置数据库timestamp数据的自动插入
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.setFieldValByName("orderdate", new Date(), metaObject);
        this.setFieldValByName("ordertime", new Date(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (((Integer) this.getFieldValByName("status", metaObject)).equals(6)) {
            this.setFieldValByName("canceltime", new Date(), metaObject);
        }
    }
}
