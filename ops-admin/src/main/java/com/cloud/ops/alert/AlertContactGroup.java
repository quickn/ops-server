package com.cloud.ops.alert;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.ops.base.MultiTenantCreateByEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报警联系人组
 *
 * <p>一组联系人的集合，预警规则关联联系人组来指定接收人。</p>
 *
 * @author Liuyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_contact_group")
public class AlertContactGroup extends MultiTenantCreateByEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "组名称")
    private String groupName;

    @Schema(description = "联系人ID列表，逗号分隔")
    private String contactIds;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人名称")
    private String createByName;

}
