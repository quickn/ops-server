package com.youlai.monitor.system;

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
@SelectSql(" * from system_info ")
@OrderBy(" id ")
public class SystemInfoQuery extends PageForm implements IQuery {
    @Where
    private Integer serviceId;
}
