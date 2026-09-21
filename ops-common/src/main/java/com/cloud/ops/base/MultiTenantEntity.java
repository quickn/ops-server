package com.cloud.ops.base;

/**
 * 多租户隔离实体标记接口。
 *
 * <p>实现该接口的实体表带有 create_by 列。在 MyBatis-Plus 标准查询方法中，
 * 非超级管理员用户会被自动追加 {@code create_by = 当前登录用户ID} 的过滤条件，
 * 保证用户只能查询到自己创建的数据。</p>
 */
public interface MultiTenantEntity {

    /**
     * 创建人ID
     */
    Long getCreateBy();
}
