package com.cloud.ops.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MultiTenantCreateByEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报警联系人
 *
 * @author Liuyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_contact")
public class AlertContact extends MultiTenantCreateByEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "备注")
    private String remark;
}
