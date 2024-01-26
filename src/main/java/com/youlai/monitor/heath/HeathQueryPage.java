package com.youlai.monitor.heath;

import com.youlai.base.sql.IQuery;
import com.youlai.base.sql.PageForm;
import com.youlai.base.sql.annotation.OrderBy;
import com.youlai.base.sql.annotation.SelectSql;
import com.youlai.base.sql.annotation.Where;
import lombok.Data;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
@SelectSql(" * from heath_monitor ")
@OrderBy(" id desc")
public class HeathQueryPage extends PageForm implements IQuery {

    @Where
    private String appName;
}
