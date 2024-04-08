package com.bszn.monitor.log;

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
@SelectSql(" * from log_info ")
@OrderBy(" id desc ")
public class LogInfoQuery extends PageForm implements IQuery {
    @Where
    private Integer serviceId;

    @Where
    private String hostname;

    @Where
    private String title;
}
