package com.cloud.ops.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.cloud.ops.base.MonitorBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 流量状态
 * Created by Liuyun on 2024-01-04 11:12
 **/
@Data
public class NetworkState extends MonitorBaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String hostname;

    @ApiModelProperty("进程名")
    private String processName;

    @ApiModelProperty("发送流量")
    private Double sent;

    @ApiModelProperty("接收流量")
    private Double received;
}
