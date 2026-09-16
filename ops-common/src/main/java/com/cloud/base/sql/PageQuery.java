package com.cloud.base.sql;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class PageQuery<T> extends Page<T> {

    public PageQuery(long current, long size) {
        super(current, size, 0L);
    }

    public PageQuery() {
        super();
    }

}
