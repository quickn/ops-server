package com.bszn.monitor.heath;

import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import com.bszn.base.sql.annotation.OrderBy;
import com.bszn.base.sql.annotation.SelectSql;
import com.bszn.base.sql.annotation.Where;
import lombok.Data;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
@SelectSql(" * from heath_monitor ")
@OrderBy(" id desc")
public class ApiHeathQueryPage extends PageForm implements IQuery {

    @Where
    private String appName;
}
