package com.bszn.base.sql;

import com.bszn.base.sql.annotation.Where;
import lombok.Data;

/**
 * Created by Liuyun on 2023-09-09 11:53
 **/
@Data
public class PageForm {

    @Where(ignore = true)
    private Integer pageNum = 1;

    @Where(ignore = true)
    private Integer pageSize = 10;
}
