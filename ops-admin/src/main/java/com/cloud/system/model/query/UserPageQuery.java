package com.cloud.system.model.query;

import com.cloud.system.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户分页查询对象
 *
 * @author haoxr
 * @since 2022/1/14
 */
@Schema
@Data
public class UserPageQuery extends BasePageQuery {

    @Schema(description="关键字(用户名/昵称/手机号)")
    private String keywords;

    @Schema(description="用户状态")
    private Integer status;

    @Schema(description="部门ID")
    private Long deptId;

    /**
     * 当前登录用户ID(用于多租户数据隔离，非超级管理员只能查看自己及自己创建的用户)
     * 为空表示无数据权限限制(超级管理员)
     */
    @Schema(description="当前登录用户ID(多租户过滤条件，内部使用)", hidden = true)
    private Long currentUserId;

}