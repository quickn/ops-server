package com.bszn.base.sql.annotation;

import java.lang.annotation.*;

/***
 * 前置sql注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OrderBy {
    String value();
}
