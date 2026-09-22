package com.cloud.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * 用户登录视图对象
 *
 * @author haoxr
 * @since 2022/1/14
 */
@Schema(description = "当前登录用户视图对象")
@Data
public class UserInfoVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "用户角色编码集合")
    private Set<String> roles;

    @Schema(description = "用户权限标识集合")
    private Set<String> perms;


    @Schema(description = "当前部门用户")
    private Set<Long> deptUsers;

    @Schema(description = "数据范围(1-全部数据权限,2-自定义数据权限,3-本部门数据权限,4-本部门及以下数据权限,5-仅本人数据权限)")
    private Integer dataScope;

}
