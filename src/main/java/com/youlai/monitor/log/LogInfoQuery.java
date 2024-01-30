package com.youlai.monitor.log;

import com.youlai.base.sql.IQuery;
import com.youlai.base.sql.PageForm;
import com.youlai.base.sql.annotation.OrderBy;
import com.youlai.base.sql.annotation.SelectSql;
import com.youlai.base.sql.annotation.Where;
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
