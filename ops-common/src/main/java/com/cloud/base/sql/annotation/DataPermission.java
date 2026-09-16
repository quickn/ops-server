package com.cloud.base.sql.annotation;

import java.lang.annotation.*;

/**
 * Created by Liuyun on 2022-05-14 8:47
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataPermission {

    String tableAlias() default "";

    /**
     * 忽略已删除数据
     *
     * @return
     */
    boolean ignoreDelete() default false;

    /**
     * 忽略所有权限控制
     *
     * @return
     */
    boolean ignoreAll() default false;
}
