package com.cloud.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 给用户分配服务表单
 *
 * @author liuyun
 * @since 2026-09-18
 */
@Schema(description = "给用户分配服务表单")
@Data
public class UserServicesForm {

    @Schema(description = "目标用户ID")
    private Long userId;

    @Schema(description = "服务ID集合")
    private List<Integer> serviceIds;
}
