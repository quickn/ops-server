package com.cloud.receiver.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 8698319936744959815L;

    @TableField(fill = FieldFill.INSERT)
    private Integer serviceId;

    private String serviceName;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

}
