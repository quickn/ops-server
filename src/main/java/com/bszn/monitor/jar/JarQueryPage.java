package com.bszn.monitor.jar;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bszn.base.sql.IQuery;
import com.bszn.base.sql.PageForm;
import com.bszn.base.sql.annotation.OrderBy;
import com.bszn.base.sql.annotation.SelectSql;
import com.bszn.base.sql.annotation.Where;
import lombok.Data;

import java.util.Objects;

/**
 * Created by Liuyun on 2023-09-09 11:51
 **/
@Data
public class JarQueryPage extends PageForm implements IQuery {

    private String fileName;

    private Integer status; // 0-未部署 1-部署中 2-部署成功 3-部署失败

    private Integer serviceId;


    public Page<JarPackage> getPage() {
        return new Page<>(super.getPageNum(), super.getPageSize());
    }

    /**
     * 链式wrapper构建
     */
    public LambdaQueryWrapper<JarPackage> buildLambda() {
        return Wrappers.<JarPackage>lambdaQuery()
                .like(StrUtil.isNotEmpty(fileName), JarPackage::getFileName, fileName)
                .eq(Objects.nonNull(serviceId), JarPackage::getServiceId, serviceId)
                .eq(Objects.nonNull(status), JarPackage::getStatus, status);
    }

}
