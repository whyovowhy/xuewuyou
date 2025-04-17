package com.tianji.learning.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MybatisConfiguration {
    //动态表名拦截器插件       如果对points_board表进行crud会被拦截器替换为TableInfoContext.getinfo（）
    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
        // 准备一个Map，用于存储TableNameHandler
        Map<String, TableNameHandler> map = new HashMap<>(1);
        // 存入一个TableNameHandler，用来替换points_board表名称
        // 替换方式，就是从TableInfoContext中读取保存好的动态表名
//        map.put("points_board", (sql, tableName) -> {
//            return TableInfoContext.getInfo() == null ? tableName : TableInfoContext.getInfo();
//        });
        map.put("points_board", new TableNameHandler() {
            @Override
            public String dynamicTableName(String sql, String tableName) {
                return tableName==null?tableName:TableInfoContext.getInfo();
            }
        });
        return new DynamicTableNameInnerInterceptor(map);
    }
}