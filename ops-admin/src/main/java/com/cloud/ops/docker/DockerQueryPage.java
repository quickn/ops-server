package com.cloud.ops.docker;

import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import com.cloud.base.sql.annotation.OrderBy;
import com.cloud.base.sql.annotation.SelectSql;
import com.cloud.base.sql.annotation.Where;
import com.cloud.base.sql.enums.MySqlKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Where(sqlkeyWord = MySqlKeyword.EQ)
    private String hostname;

    @Where(sqlkeyWord = MySqlKeyword.LIKE)
    private String names;

    @Schema(description = "名称全等")
    @Where(sqlkeyWord = MySqlKeyword.EQ,columnName = "names")
    private String namesEq;

}
