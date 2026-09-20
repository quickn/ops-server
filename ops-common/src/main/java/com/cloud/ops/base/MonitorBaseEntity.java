package com.cloud.ops.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 监控采集类库表公共基类。
 *
 * <p>适用于 cpu_state / mem_state / sys_load_state / netio_state / network_state /
 * app_info / app_state 等采集数据表：这些表带 service_id / service_name / create_time / id，
 * 但不带 update_time，因此这里不映射 updateTime。</p>
 */
@Data
public class MonitorBaseEntity implements Serializable {

    private static final long serialVersionUID = 8698319936744959815L;

    /**
     * 服务ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer serviceId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

}
