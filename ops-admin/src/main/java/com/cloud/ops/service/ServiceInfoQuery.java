package com.cloud.ops.service;

import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import com.cloud.base.sql.annotation.OrderBy;
import com.cloud.base.sql.annotation.SelectSql;
import com.cloud.base.sql.annotation.Where;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Liuyun on 2024-01-09 11:51
 **/
@Data
@SelectSql(" * from service_info ")
@OrderBy(" sort desc ")
@Builder
public class ServiceInfoQuery extends PageForm implements IQuery {

    @Where
    private String name;
}
