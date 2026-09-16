package com.cloud.base.sql.annotation;

import java.lang.annotation.*;

/***
 * 查询字段
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SelectColumn {
    String value();
}
