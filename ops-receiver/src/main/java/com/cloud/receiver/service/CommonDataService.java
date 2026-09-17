package com.cloud.receiver.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用数据保存服务
 * <p>
 * 不依赖 MyBatis Mapper，直接根据表名 + JSONObject 拼装 INSERT SQL 并通过 JDBC 执行。
 * </p>
 *
 * <pre>
 * 调用示例:
 *   &#064;Resource
 *   private CommonDataService commonDataService;
 *
 *   JSONObject json = new JSONObject();
 *   json.put("hostname", "192.168.1.1");
 *   json.put("cpu_use", 85.5);
 *   commonDataService.insert("cpu_state", json);
 * </pre>
 */
@Service
@Slf4j
public class CommonDataService {

    private DataSource dataSource;

    private DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = com.cloud.base.spring.ApplicationContextHelper.getBean(DataSource.class);
        }
        return dataSource;
    }

    /**
     * 根据表名将 JSONObject 插入数据库
     *
     * @param tableName 数据库表名（下划线命名），如 "cpu_state"
     * @param jsonData  待插入的数据，key 需与表字段名一致（下划线命名）
     * @return 插入成功返回 true
     */
    public boolean insert(String tableName, JSONObject jsonData) {
        if (tableName == null || tableName.isEmpty() || jsonData == null || jsonData.isEmpty()) {
            log.warn("insert 参数为空: tableName={}, jsonData={}", tableName, jsonData);
            return false;
        }
        // 保持 JSON key 的插入顺序
        Map<String, Object> fieldMap = new LinkedHashMap<>();
        for (String key : jsonData.keySet()) {
            fieldMap.put(key, jsonData.get(key));
        }
        if (fieldMap.isEmpty()) {
            return false;
        }
        return doInsert(tableName, fieldMap);
    }

    /**
     * 根据表名将 JSONArray 批量插入数据库
     *
     * @param tableName 数据库表名
     * @param jsonArray 待插入的 JSONArray，每条 JSONObject 对应一行
     * @return 成功插入的条数
     */
    public int insertBatch(String tableName, JSONArray jsonArray) {
        if (tableName == null || tableName.isEmpty() || jsonArray == null || jsonArray.isEmpty()) {
            log.warn("insertBatch 参数为空: tableName={}, jsonArray={}", tableName, jsonArray);
            return 0;
        }
        // 以第一条数据的 key 作为列名（保证顺序一致）
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            if (item == null || item.isEmpty()) {
                continue;
            }
            Map<String, Object> fieldMap = new LinkedHashMap<>();
            for (String key : item.keySet()) {
                fieldMap.put(key, item.get(key));
            }
            rows.add(fieldMap);
        }
        return doInsertBatch(tableName, rows);
    }

    /**
     * 单条 insert
     */
    private boolean doInsert(String tableName, Map<String, Object> fieldMap) {
        String sql = buildInsertSql(tableName, new ArrayList<>(fieldMap.keySet()));
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, fieldMap);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            log.error("insert 失败: tableName={}, data={}", tableName, fieldMap, e);
            return false;
        }
    }

    /**
     * 批量 insert（单条 SQL 多条 values）
     */
    private int doInsertBatch(String tableName, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return 0;
        }
        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        String sql = buildInsertBatchSql(tableName, columns, rows.size());
        try (Connection conn = getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Map<String, Object> row : rows) {
                for (String col : columns) {
                    ps.setObject(idx++, row.get(col));
                }
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("insertBatch 失败: tableName={}", tableName, e);
            return 0;
        }
    }

    /**
     * 拼装单条 INSERT SQL
     * <pre>INSERT INTO table_name (col1, col2) VALUES (?, ?)</pre>
     */
    private String buildInsertSql(String tableName, List<String> columns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    /**
     * 拼装批量 INSERT SQL
     * <pre>INSERT INTO table_name (col1, col2) VALUES (?, ?), (?, ?)</pre>
     */
    private String buildInsertBatchSql(String tableName, List<String> columns, int rowCount) {
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i));
        }
        sql.append(") VALUES ");
        String placeholder = "(";
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) placeholder += ", ";
            placeholder += "?";
        }
        placeholder += ")";
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) sql.append(", ");
            sql.append(placeholder);
        }
        return sql.toString();
    }

    /**
     * 设置 PreparedStatement 参数
     */
    private void setParams(PreparedStatement ps, Map<String, Object> fieldMap) throws SQLException {
        int idx = 1;
        for (Object value : fieldMap.values()) {
            ps.setObject(idx++, value);
        }
    }
}
