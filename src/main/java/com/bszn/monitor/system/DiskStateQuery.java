package com.bszn.monitor.system;

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
@SelectSql(" * from disk_state ")
@OrderBy(" id ")
public class DiskStateQuery extends PageForm implements IQuery {
    @Where
    private Integer serviceId;

    @Where
    private String hostname;

    @Where(ignore = true)
    private String sortField;

    @Where(ignore = true)
    private String sortOrder;
}
