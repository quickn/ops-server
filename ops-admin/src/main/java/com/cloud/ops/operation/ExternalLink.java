package com.cloud.ops.operation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.cloud.base.ServiceBaseEntity;
import lombok.Data;

@Data
public class ExternalLink extends ServiceBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String url;

    private Boolean isInternalOpen;

}
