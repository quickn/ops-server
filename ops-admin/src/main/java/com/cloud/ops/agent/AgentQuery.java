package com.cloud.ops.agent;

import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import com.cloud.base.sql.annotation.OrderBy;
import com.cloud.base.sql.annotation.SelectSql;
import com.cloud.base.sql.annotation.Where;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by Liuyun on 2024-01-09 11:51
 **/
@Data
@SelectSql(" * from agent ")
@OrderBy(" id ")
public class AgentQuery extends PageForm implements IQuery {

    public AgentQuery() {

    }
    public AgentQuery(Integer serviceId) {
        this.serviceId = serviceId;
    }

    @Where
    @Schema(description = "服务ID")
    private Integer serviceId;

    @Where(ignore = true)
    @Schema(description = "容器名称")
    private String dockerName;

    @Where
    @Schema(description = "是否跳板机")
    private Boolean isJumpServer;

    @Where
    @Schema(description = "是否是源服务")
    private Boolean isOriginServer;

    @Where
    @Schema(description = "是否在线")
    private Boolean online;
}
