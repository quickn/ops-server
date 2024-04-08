package com.bszn.base.sql.annotation;

import com.bszn.base.sql.enums.MySqlKeyword;
import java.lang.annotation.*;

/***
 * 配置与字段查询条件，字段名称，是否忽略处理
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Where {

    /**
     * 条件关键字
     *
     * @return
     */
    MySqlKeyword sqlkeyWord() default MySqlKeyword.AND;

    /***
     * 表关联列表
     * @return
     */
    String columnName() default "";


    /***
     * 表别名
     * @return
     */
    String alias() default "";


    /***
     * 忽略当前字段
     * @return
     */
    boolean ignore() default false;

    String sql() default "";


}
