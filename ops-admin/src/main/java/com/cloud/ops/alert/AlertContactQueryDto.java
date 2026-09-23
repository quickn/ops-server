package com.cloud.ops.alert;

import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Objects;

/**
 * @author Liuyun
 */
@Data
public class AlertContactQueryDto extends PageForm<AlertContact> implements IQuery {

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Override
    public com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AlertContact> buildLambda() {
        return super.buildLambda()
                .like(Objects.nonNull(contactName) && !contactName.isEmpty(), AlertContact::getContactName, contactName)
                .like(Objects.nonNull(email) && !email.isEmpty(), AlertContact::getEmail, email)
                .like(Objects.nonNull(phone) && !phone.isEmpty(), AlertContact::getPhone, phone)
                .orderByDesc(AlertContact::getId);
    }
}
