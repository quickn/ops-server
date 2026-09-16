package com.cloud.ops.processnetstat;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 进程统计 Mapper
 *
 * @author Liuyun
 */
@Mapper
public interface ProcessStatMapper extends BaseQueryMapper<ProcessStat, ProcessStat> {

    /**
     * 清理指定时间之前的历史数据
     */
    @Delete({" delete from process_stat where create_time <= #{createTime} "})
    int deleteByDate(@Param("createTime") String createTime);
}
