package com.bszn.monitor.service;

import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import com.bszn.base.sql.annotation.OrderBy;
import com.bszn.base.sql.annotation.SelectSql;
import com.bszn.base.sql.annotation.Where;
import lombok.Data;

/**
 * Created by Liuyun on 2024-01-09 11:51
 **/
@Data
@SelectSql(" * from service_info ")
@OrderBy(" sort desc ")
public class ServiceInfoQuery extends PageForm implements IQuery {

    @Where
    private String name;
}
