package com.bszn.monitor.jar;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import lombok.Data;

import java.util.Objects;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
public class JarQueryPage extends PageForm<JarPackage> implements IQuery {

    private String fileName;

    private Integer serviceId;

    /**
     * 链式wrapper构建
     */
    public LambdaQueryWrapper<JarPackage> buildLambda() {
        return Wrappers.<JarPackage>lambdaQuery()
                .like(StrUtil.isNotEmpty(fileName), JarPackage::getFileName, fileName)
                .eq(Objects.nonNull(serviceId), JarPackage::getServiceId, serviceId);
    }

    /**
     * 链式wrapper构建
     */
    public QueryWrapper<JarPackage> build() {
        return Wrappers.<JarPackage>query()
                .like(StrUtil.isNotEmpty(fileName), "jp1.file_name", fileName)
                .eq(Objects.nonNull(serviceId), "jp1.service_id", serviceId);
    }

}
