package com.bszn.base.sql;

import lombok.Data;

/**
 * 自定义查询语句
 * Created by Liuyun on 2023-06-19 11:43
 **/
@Data
public class QueryParam {

    private String selectSql;

    private boolean isMultiplePara = true;
}
