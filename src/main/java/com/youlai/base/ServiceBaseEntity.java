package com.youlai.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class ServiceBaseEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     *
     */
    private static final long serialVersionUID = 8698319936744959815L;

    @TableField(fill = FieldFill.INSERT)
    private Integer serviceId;

    private String serviceName;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

}
