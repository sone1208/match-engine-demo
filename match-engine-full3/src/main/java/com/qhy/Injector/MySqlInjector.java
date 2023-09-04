package com.qhy.Injector;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;
import com.qhy.Injector.method.UpdateBatchMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MySqlInjector extends DefaultSqlInjector {

    /**
     * 注入InsertBatchSomeColumn，实现真正意义上的批量插入
     * 注入UpdateBatchMethod，实现真正意义上的批量更新
     * @param mapperClass 当前mapper
     * @param tableInfo
     * @return
     */
    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
        //增加自定义方法，字段注解上等于FieldFill.DEFAULT的字段才会插入
        methodList.add(new InsertBatchSomeColumn(i -> i.getFieldFill() == FieldFill.DEFAULT));
        methodList.add(new UpdateBatchMethod("updateBatch"));
        return methodList;
    }
}
