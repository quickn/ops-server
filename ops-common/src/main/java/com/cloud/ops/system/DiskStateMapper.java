package com.cloud.ops.system;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface DiskStateMapper extends BaseQueryMapper<DiskState, DiskState> {

    /**
     * 清理指定时间之前的历史数据
     */
    @Delete({" delete from disk_state where create_time <= #{createTime} "})
    int deleteByDate(@Param("createTime") String createTime);
}
