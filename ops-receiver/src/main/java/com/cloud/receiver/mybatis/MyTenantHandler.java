package com.cloud.receiver.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 多租户处理器
 * Created by Liuyun on 2022-03-17 10:15
 **/
@Slf4j
@Component
@ConfigurationProperties(prefix = "tenant")
public class MyTenantHandler implements TenantLineHandler {

    /**
     * 需要过滤的表tenant
     */
    private List<String> ignoreTables = new ArrayList<>();

    public Integer getCompanyId() {
        return null;
    }

    @Override
    public Expression getTenantId() {
        Integer companyId = this.getCompanyId();
        if (companyId == null) {
            return new IsNullExpression();
        }
        return new LongValue(companyId);
    }

    @Override
    public String getTenantIdColumn() {
        return "service_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        Integer companyId = this.getCompanyId();
        if ("p_parking_lot".equals(tableName)) {
            return false;
        }
        if (companyId == null)
            return true;
        return ignoreTables.stream().anyMatch((e) -> e.equalsIgnoreCase(tableName));
    }

    public List<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(List<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }
}
