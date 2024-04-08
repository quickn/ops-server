package com.bszn.monitor.docker;

import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import com.bszn.base.sql.annotation.OrderBy;
import com.bszn.base.sql.annotation.SelectSql;
import com.bszn.base.sql.annotation.Where;
import com.bszn.base.sql.enums.MySqlKeyword;
import lombok.Data;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
@SelectSql(" * from docker_container ")
@OrderBy(" id desc")
public class DockerQueryPage extends PageForm implements IQuery {

    @Where
    private Integer serviceId;

    @Where
    private String serviceName;

    @Where
    private String hostname;

    @Where(sqlkeyWord = MySqlKeyword.LIKE)
    private String names;

}
