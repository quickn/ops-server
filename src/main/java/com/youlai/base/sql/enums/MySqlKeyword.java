package com.youlai.base.sql.enums;

/**
 * Created by Liuyun on 2021-12-28 8:17
 **/
public enum MySqlKeyword {
    APPEND("append"),
    AND("AND"),
    OR("OR"),
    NOT("NOT"),
    IN("IN"),
    NOT_IN("NOT IN"),
    LIKE("LIKE"),
    LIKE_Underline(" LIKE "),//下划线
    REGEXP(" regexp "),//正则函数
    NOT_LIKE("NOT LIKE"),
    EQ("="),
    NE("<>"),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    GROUP_BY("GROUP BY"),
    HAVING("HAVING"),
    ORDER_BY("ORDER BY"),
    EXISTS("EXISTS"),
    NOT_EXISTS("NOT EXISTS"),
    BETWEEN("BETWEEN"),
    NOT_BETWEEN("NOT BETWEEN"),
    ASC("ASC"),
    DESC("DESC");

    private final String keyword;

    public String getSqlSegment() {
        return this.keyword;
    }

    private MySqlKeyword(final String keyword) {
        this.keyword = keyword;
    }
}
