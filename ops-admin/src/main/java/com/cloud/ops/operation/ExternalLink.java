package com.cloud.ops.operation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.cloud.ops.base.MonitorBaseEntity;
import lombok.Data;

@Data
public class ExternalLink extends MonitorBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String url;

    private Boolean isInternalOpen;

}
