package com.bszn.base.sql;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bszn.base.sql.annotation.*;
import com.bszn.base.sql.enums.MySqlKeyword;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.annotation.ProviderMethodResolver;
import org.apache.ibatis.jdbc.SQL;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseProviderMethodResolver implements ProviderMethodResolver {

    public String queryPage(IQuery param1, IPage page) {
        QueryParam queryParam = new QueryParam();
        queryParam.setMultiplePara(true);
        String sql = getSql(param1, queryParam).toString();
        return sql;
    }


    public String queryPageIgnoreDelete(IQuery param1, IPage page) {
        QueryParam queryParam = new QueryParam();
        queryParam.setMultiplePara(true);
        String sql = getSql(param1, queryParam).toString();
        return sql;
    }

    public String queryIgnoreDelete(IQuery param1) {
        QueryParam queryParam = new QueryParam();
        queryParam.setMultiplePara(false);
        String sql = getSql(param1, queryParam).toString();
        return sql;
    }

    public String qPage(IQuery param1, IPage page) {
        QueryParam queryParam = new QueryParam();
        queryParam.setMultiplePara(true);
        String sql = getSql(param1, queryParam).toString();
        return sql;
    }

    public String query(IQuery param1) {
        QueryParam queryParam = new QueryParam();
        queryParam.setMultiplePara(false);
        return getSql(param1, queryParam).toString();
    }

    public String queryObj(IQuery param1, QueryParam param2) {
        param2.setMultiplePara(true);
        String sql = getSql(param1, param2).toString();
        return sql;
    }

    public String queryTotal(IQuery param1, QueryParam param2) {
        return getSql(param1, param2).toString();
    }

    public String queryOne(IQuery param1) {
        QueryParam queryParam = new QueryParam();
        queryParam.setMultiplePara(false);
        String sql = getSql(param1, queryParam).toString();
        return sql;
    }

    public SQL getSql(IQuery query, QueryParam queryParam) {
        return new SQL() {{
            SELECT(getSelectSql(query, queryParam));
            Map<String, Object> map = getWhereMap(query, queryParam);
            //对map进行倒序遍历
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            boolean isSub = false;
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                String value = entry.getValue().toString();
                if (entry.getKey().startsWith(MySqlKeyword.APPEND.getSqlSegment())) {
                    WHERE(entry.getValue().toString());
                } else {
                    if (entry.getKey().equals(MySqlKeyword.NOT_EXISTS.getSqlSegment()) && iterator.hasNext()) {
                        value = value.substring(0, value.length() - 1);
                        isSub = true;
                    }
                    if (!iterator.hasNext() && isSub) {
                        value += ")";
                    }
                    WHERE(entry.getKey() + value);
                }
            }
            if (getGroupBy(query) != null) {
                GROUP_BY(getGroupBy(query));
            }
            if (getOrderBy(query) != null) {
                ORDER_BY(getOrderBy(query));
            }
        }};
    }


    static Pattern humpPattern = Pattern.compile("[A-Z]");

    /**
     * 驼峰转下划线,效率比上面高
     */
    public static String humpToLine2(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    public static String getOrderBy(Object query) {
        OrderBy orderBy = query.getClass().getAnnotation(OrderBy.class);
        if (orderBy != null) {
            return orderBy.value();
        }
        return null;
    }

    public static String getGroupBy(Object query) {
        GroupBy groupBy = query.getClass().getAnnotation(GroupBy.class);
        if (groupBy != null) {
            return groupBy.value();
        }
        return null;
    }

    public static String getSelectSql(Object query, QueryParam queryParam) {
        if (queryParam.getSelectSql() != null) {
            return queryParam.getSelectSql();
        }
        SelectSql selectSql = query.getClass().getAnnotation(SelectSql.class);
        if (selectSql != null) {
            return selectSql.value();
        }
        SelectColumn selectColumn = query.getClass().getAnnotation(SelectColumn.class);
        SelectFrom selectFrom = query.getClass().getAnnotation(SelectFrom.class);
        if (selectColumn != null && selectFrom != null) {
            return selectColumn.value() + selectFrom.value();
        }
        if (selectSql == null) {
            throw new SqlException("未配置前置Select");
        }
        return selectSql.value();
    }

    /***
     * 关键字为空设置默认的关键字
     * @param typeName
     * @param fieldName
     * @return
     */
    private MySqlKeyword getSqlKeywordByTypeName(String typeName, String fieldName) {
        MySqlKeyword sqlKeyword = MySqlKeyword.EQ;
        if (typeName.contains("String")) {
            sqlKeyword = MySqlKeyword.LIKE;
        }
        if (fieldName.endsWith("Time") && fieldName.startsWith("start")) sqlKeyword = MySqlKeyword.GE;
        if (fieldName.endsWith("Time") && fieldName.startsWith("end")) sqlKeyword = MySqlKeyword.LE;
        return sqlKeyword;
    }

    private String getColumnNameByField(Field field) {
        Where columnDb = field.getAnnotation(Where.class);
        String columnName = null;
        String alias = "";
        if (columnDb != null) {
            columnName = columnDb.columnName().trim();
            alias = columnDb.alias().trim();
            if (StringUtils.isNotEmpty(alias)) alias = alias + ".";
            if (StringUtils.isNotEmpty(columnName)) return alias + columnName;
        }
        String fieldName = field.getName();//根据字段转换
        columnName = humpToLine2(fieldName);
        if (columnName.startsWith("start_")) {
            columnName = columnName.replace("start_", "");
        } else if (columnName.startsWith("end_")) {
            columnName = columnName.replace("end_", "");
        }
        return alias + columnName;
    }

    /***
     * 组装查询where条件
     * @param query
     * @return
     */
    public Map<String, Object> getWhereMap(Object query, QueryParam queryParam) {
        Map<String, Object> map = BeanUtil.beanToMap(query);
        // Map<String, Object> map = JSONObject.parseObject(JSONObject.toJSONString(query), Map.class);
        Map<String, Object> newMap = new LinkedHashMap<>();
        //可以获取当前类的私有化属性值，公有化属性
        Field[] fields = query.getClass().getDeclaredFields();
        if (fields.length == 0) {
            //判断一个类是否实现了某个接口
            if (IQuery.class.isAssignableFrom(query.getClass().getSuperclass())) {
                //能取到继承的父类公有属性，私有属性无法获取
                fields = query.getClass().getFields();
            }
        }
        int i = 0;
        for (Field field : fields) {
            String fieldName = field.getName();
            Where columnDb = field.getAnnotation(Where.class);
            MySqlKeyword sqlKeyword = MySqlKeyword.EQ;
            String columnName = getColumnNameByField(field);
            String typeName = field.getGenericType().getTypeName();
            //处理为空值的字段
            Object value = map.get(fieldName);
            if (columnDb == null) {
                sqlKeyword = getSqlKeywordByTypeName(typeName, fieldName);
            } else {
                sqlKeyword = columnDb.sqlkeyWord();
                if (sqlKeyword.getSqlSegment().equals(SqlKeyword.AND.getSqlSegment()))//未设置
                    sqlKeyword = getSqlKeywordByTypeName(typeName, fieldName);
                boolean ignore = columnDb.ignore();
                if (ignore) {
                    continue;
                }
                if (StringUtil.isNotEmpty(columnDb.sql()) && MySqlKeyword.APPEND.getSqlSegment().equals(sqlKeyword.getSqlSegment())) {
                    if (value != null) {
                        newMap.put(MySqlKeyword.APPEND.getSqlSegment() + i, columnDb.sql());
                        i++;
                    }
                    continue;
                }
            }
            if (MySqlKeyword.IS_NULL.getSqlSegment().equals(sqlKeyword.getSqlSegment()) || MySqlKeyword.IS_NOT_NULL.getSqlSegment().equals(sqlKeyword.getSqlSegment())) {
                if (value == null) {
                    continue;
                }
                newMap.put(columnName, " " + sqlKeyword.getSqlSegment());
                continue;
            }
            if (value == null || StringUtil.isEmpty(value.toString())) {//值为空不作为查询条件
                if (MySqlKeyword.NOT_EXISTS.getSqlSegment().equals(sqlKeyword.getSqlSegment())) {
                    newMap.put(sqlKeyword.getSqlSegment(), "(" + columnDb.sql() + ")");
                }
                continue;
            }
            StringBuffer condition = new StringBuffer();
            String sqlSegment = sqlKeyword.getSqlSegment();
            if (sqlSegment.equals(MySqlKeyword.LIKE.getSqlSegment())) {
                condition.append(" concat('%',");
            }
            if (queryParam.isMultiplePara()) {
                condition.append("#{param1.");
            } else {
                condition.append("#{");
            }
            condition.append(fieldName);
            condition.append("}");
            if (sqlSegment.equals(MySqlKeyword.LIKE.getSqlSegment())) {
                condition.append(",'%')");
            }
            if (MySqlKeyword.APPEND.getSqlSegment().equals(sqlKeyword.getSqlSegment())) {
                newMap.put(sqlKeyword.getSqlSegment(), value.toString());
            } else if (MySqlKeyword.NOT_IN.getSqlSegment().equals(sqlKeyword.getSqlSegment())) {
                newMap.put(columnName + " " + sqlKeyword.getSqlSegment(), "(" + value.toString() + ")");
            } else if (MySqlKeyword.IN.getSqlSegment().equals(sqlKeyword.getSqlSegment())) {
                newMap.put(columnName + " " + sqlKeyword.getSqlSegment(), "(" + value.toString() + ")");
            } else {
                newMap.put(columnName + " " + sqlKeyword.getSqlSegment(), condition.toString());
            }
        }
        return newMap;
    }

    public String selectById(Long id, String keyName, String tableName) {
        SQL sql = new SQL() {
            {
                SELECT(" * ");
                FROM(tableName);
            }
        };
        sql.WHERE(keyName + " = " + id);
        return sql.toString();
    }

}
