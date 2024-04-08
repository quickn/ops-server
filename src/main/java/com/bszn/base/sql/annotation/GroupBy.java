package com.bszn.base.sql.annotation;

import java.lang.annotation.*;

/***
 * group 注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GroupBy {
    String value();
}
