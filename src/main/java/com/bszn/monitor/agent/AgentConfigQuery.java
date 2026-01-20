package com.bszn.monitor.agent;

import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import com.bszn.base.sql.annotation.OrderBy;
import com.bszn.base.sql.annotation.SelectSql;
import com.bszn.base.sql.annotation.Where;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Liuyun on 2024-01-09 11:51
 **/
@Data
@SelectSql(" * from agent_config ")
@OrderBy(" id ")
@Builder
public class AgentConfigQuery extends PageForm implements IQuery {
    @Where
    private Integer serviceId;

    @Where(ignore = true)
    @Schema(description = "容器名称")
    private String dockerName;
}
