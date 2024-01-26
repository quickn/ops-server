package com.youlai.monitor.docker;

import com.youlai.base.sql.IQuery;
import com.youlai.base.sql.PageForm;
import com.youlai.base.sql.annotation.OrderBy;
import com.youlai.base.sql.annotation.SelectSql;
import com.youlai.base.sql.annotation.Where;
import com.youlai.base.sql.enums.MySqlKeyword;
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
