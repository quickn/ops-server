package com.cloud.base.mapper;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.base.spring.SpringServletUtil;
import com.cloud.base.sql.BaseProviderMethodResolver;
import com.cloud.base.sql.IQuery;
import com.cloud.base.sql.PageQuery;
import com.cloud.base.sql.QueryParam;
import com.cloud.system.common.annotation.DataPermission;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

/**
 * <p>
 * 通道配置表 Mapper 接口
 * </p>
 *
 * @author liuyun
 * @since 2021-10-22
 */
public interface BaseQueryMapper<T, V> extends BaseMapper<T> {

    @SelectProvider(type = BaseProviderMethodResolver.class)
    @DataPermission
    <E> E queryTotal(IQuery param1, QueryParam param2);

    @SelectProvider(type = BaseProviderMethodResolver.class)
    @DataPermission
    <E> List<E> queryObj(IQuery param1, QueryParam param2);

    @SelectProvider(type = BaseProviderMethodResolver.class)
    @DataPermission
    List<V> query(IQuery param1);

    @SelectProvider(type = BaseProviderMethodResolver.class)
    @DataPermission
    PageQuery<V> queryPage(IQuery param1, IPage<T> page);

    @SelectProvider(type = BaseProviderMethodResolver.class)
        //@DataPermission(ignoreDelete = true)
    PageQuery<V> queryPageIgnoreDelete(IQuery param1, IPage<T> page);

    @SelectProvider(type = BaseProviderMethodResolver.class)
        // @DataPermission(ignoreDelete = true)
    List<V> queryIgnoreDelete(IQuery param1);

    @SelectProvider(type = BaseProviderMethodResolver.class)
    T queryOne(IQuery param1);

    @SelectProvider(type = BaseProviderMethodResolver.class)
    @DataPermission
    IPage<V> qPage(IQuery param1, IPage<T> page);

    default IPage<T> wrapPage(Wrapper<T> queryWrapper) {
        return this.selectPage(getPage(), queryWrapper);
    }

    default PageQuery<T> getPage() {
        HttpServletRequest httpServletRequest = SpringServletUtil.getHttpServletRequest();
        JSONObject parametersMap = SpringServletUtil.getParameters(httpServletRequest);
        Integer current = parametersMap.getInt("pageNum");
        if (current == null) current = parametersMap.getInt("current");
        if (current == null) current = 1;
        Integer size = parametersMap.getInt("pageSize");
        if (size == null) size = parametersMap.getInt("size");
        if (size == null) {
            size = 10;
        }
        PageQuery<T> page = new PageQuery<T>(current, size);
        return page;
    }

}
