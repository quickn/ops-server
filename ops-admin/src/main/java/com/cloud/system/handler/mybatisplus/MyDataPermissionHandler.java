package com.cloud.system.handler.mybatisplus;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.cloud.base.mapper.BaseQueryMapper;
import com.cloud.base.spring.ApplicationContextHelper;
import com.cloud.ops.base.MonitorBaseEntity;
import com.cloud.system.common.annotation.DataPermission;
import com.cloud.system.common.base.IBaseEnum;
import com.cloud.system.common.enums.DataScopeEnum;
import com.cloud.system.common.util.SecurityUtils;
import com.cloud.system.mapper.SysDeptServiceMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限控制器
 *
 * @author zc
 * @since 2021-12-10 13:28
 */
@Slf4j
public class MyDataPermissionHandler implements DataPermissionHandler {

    /**
     * 需要按 serviceId 做数据过滤的标准查询方法白名单。
     * <p>
     * 仅对这些方法（BaseQueryMapper 的 query 系列 + MyBatis-Plus BaseMapper 的标准查询）
     * 自动附加 service_id 过滤，避免对自定义 @Select 方法（可能含 join 导致 service_id 歧义）误过滤。
     */
    private static final Set<String> SERVICE_FILTER_METHODS = Set.of(
            // BaseQueryMapper 查询方法
            "query", "queryPage", "qPage", "queryObj", "queryTotal", "queryOne",
            // MyBatis-Plus BaseMapper 标准查询方法
            "selectList", "selectPage", "selectById", "selectBatchIds", "selectOne",
            "selectCount", "selectMaps", "selectObjs", "selectMap", "selectMapsPage"
    );

    @Override
    @SneakyThrows
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 超级管理员不受数据权限控制
        if (SecurityUtils.isRoot()) {
            return where;
        }
        Class<?> clazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(StringPool.DOT)));
        String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(StringPool.DOT) + 1);

        // 继承 ServiceBaseEntity 的实体，按 serviceId 做数据过滤
        Class<?> entityClass = resolveEntityClass(clazz);
        if (entityClass != null && MonitorBaseEntity.class.isAssignableFrom(entityClass)
                && SERVICE_FILTER_METHODS.contains(methodName)) {
            return serviceIdFilter(where);
        }

        // 查找当前 Mapper 方法上的 @DataPermission 注解（部门数据范围过滤）
        DataPermission annotation = findDataPermission(clazz, methodName);
        if (annotation != null) {
            return dataScopeFilter(annotation.deptAlias(), annotation.deptIdColumnName(), annotation.userAlias(), annotation.userIdColumnName(), where);
        }

        return where;
    }

    /**
     * 查找当前 Mapper 接口自身声明的方法上的 {@link DataPermission} 注解。
     * <p>
     * 仅查询当前接口 {@link Class#getDeclaredMethods()}，不向上递归父接口，
     * 避免误启用 {@code BaseQueryMapper} 上的部门数据范围注解（业务实体无 dept_id/create_by 列）。
     */
    private DataPermission findDataPermission(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            DataPermission annotation = method.getAnnotation(DataPermission.class);
            if (ObjectUtils.isNotEmpty(annotation)
                    && (method.getName().equals(methodName) || (method.getName() + "_COUNT").equals(methodName))) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 解析 Mapper 接口对应的实体类型。
     * <p>
     * 优先从 {@link BaseQueryMapper} 的第一个泛型参数获取实体类型，
     * 兜底从 {@link BaseMapper} 的第一个泛型参数获取。
     */
    private Class<?> resolveEntityClass(Class<?> mapperClass) {
        Set<Type> visited = new HashSet<>();
        return doResolveEntityClass(mapperClass, visited);
    }

    private Class<?> doResolveEntityClass(Class<?> clazz, Set<Type> visited) {
        if (clazz == null || clazz == Object.class || !visited.add(clazz)) {
            return null;
        }
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                Type rawType = parameterizedType.getRawType();
                if (rawType == BaseQueryMapper.class || rawType == BaseMapper.class) {
                    Type[] args = parameterizedType.getActualTypeArguments();
                    if (args.length > 0 && args[0] instanceof Class) {
                        return (Class<?>) args[0];
                    }
                }
                // 递归处理父泛型接口
                if (rawType instanceof Class) {
                    Class<?> result = doResolveEntityClass((Class<?>) rawType, visited);
                    if (result != null) {
                        return result;
                    }
                }
            } else if (genericInterface instanceof Class) {
                Class<?> result = doResolveEntityClass((Class<?>) genericInterface, visited);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 按 serviceId 过滤：仅显示当前登录用户所在部门有权限的 service 数据。
     */
    private Expression serviceIdFilter(Expression where) {
        if (SecurityUtils.getUser() == null) {
            return where;
        }
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null) {
            return where;
        }
        List<Integer> serviceIds = getServiceIds(deptId);
        if (CollectionUtil.isEmpty(serviceIds)) {
            // 无任何服务权限，返回恒假条件
            String falseSql = " 1 = 0 ";
            return appendExpression(where, falseSql);
        }
        StringBuilder sb = new StringBuilder(" service_id IN ( ");
        for (int i = 0; i < serviceIds.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(serviceIds.get(i));
        }
        sb.append(" ) ");
        return appendExpression(where, sb.toString());
    }

    /**
     * 获取当前用户所在部门有权限的服务ID集合。
     */
    private List<Integer> getServiceIds(Long deptId) {
        try {
            SysDeptServiceMapper mapper = ApplicationContextHelper.getBean(SysDeptServiceMapper.class);
            return mapper.listServiceIdsByDeptId(deptId);
        } catch (Exception e) {
            log.warn("查询部门服务权限失败: deptId={}", deptId, e);
            return Collections.emptyList();
        }
    }

    private Expression appendExpression(Expression where, String appendSqlStr) {
        try {
            Expression appendExpression = CCJSqlParserUtil.parseCondExpression(appendSqlStr);
            if (where == null) {
                return appendExpression;
            }
            return new AndExpression(where, appendExpression);
        } catch (Exception e) {
            log.warn("追加数据权限条件失败: {}", appendSqlStr, e);
            return where;
        }
    }

    /**
     * 构建过滤条件
     *
     * @param where 当前查询条件
     * @return 构建后查询条件
     */
    @SneakyThrows
    public static Expression dataScopeFilter(String deptAlias, String deptIdColumnName, String userAlias, String userIdColumnName, Expression where) {

        String deptColumnName = StrUtil.isNotBlank(deptAlias) ? (deptAlias + StringPool.DOT + deptIdColumnName) : deptIdColumnName;
        String userColumnName = StrUtil.isNotBlank(userAlias) ? (userAlias + StringPool.DOT + userIdColumnName) : userIdColumnName;

        // 获取当前用户的数据权限
        Integer dataScope = SecurityUtils.getDataScope();

        DataScopeEnum dataScopeEnum = IBaseEnum.getEnumByValue(dataScope, DataScopeEnum.class);

        Long deptId, userId;
        String appendSqlStr = "";
        switch (dataScopeEnum) {
            case ALL:
                return where;
            case DEPT:
                deptId = SecurityUtils.getDeptId();
                appendSqlStr = deptColumnName + StringPool.EQUALS + deptId;
                break;
            case SELF:
                userId = SecurityUtils.getUserId();
                appendSqlStr = userColumnName + StringPool.EQUALS + userId;
                break;
            // 默认部门及子部门数据权限
            default:
                if (SecurityUtils.isRoot()) {
                    deptId = SecurityUtils.getDeptId();
                    appendSqlStr = deptColumnName + " IN ( SELECT id FROM sys_dept WHERE id = " + deptId + " OR FIND_IN_SET( " + deptId + " , tree_path ) )";
                }else {
                    if(StringUtils.isEmpty(deptAlias)){
                        userId = SecurityUtils.getUserId();
                        appendSqlStr = userColumnName + StringPool.EQUALS + userId;
                    }
                }
                break;
        }

        if (StrUtil.isBlank(appendSqlStr)) {
            return where;
        }

        Expression appendExpression = CCJSqlParserUtil.parseCondExpression(appendSqlStr);

        if (where == null) {
            return appendExpression;
        }

        return new AndExpression(where, appendExpression);
    }

}
