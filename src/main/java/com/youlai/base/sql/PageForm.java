package com.youlai.base.sql;

import com.youlai.base.sql.annotation.Where;

/**
 * Created by Liuyun on 2023-09-09 11:53
 **/
public class PageForm {

    @Where(ignore = true)
    private Integer pageNum = 1;

    @Where(ignore = true)
    private Integer pageSize = 10;
}
