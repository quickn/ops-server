package com.youlai.monitor.service;

import com.youlai.base.sql.IQuery;
import com.youlai.base.sql.annotation.OrderBy;
import com.youlai.base.sql.annotation.SelectSql;
import com.youlai.base.sql.annotation.Where;
import lombok.Data;

/**
 * Created by Liuyun on 2024-01-09 11:51
 **/
@Data
@SelectSql(" * from service_info ")
@OrderBy(" id ")
public class ServiceInfoQuery implements IQuery {

    @Where
    private String name;
}
