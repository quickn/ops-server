package com.cloud.ops.docker;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author wzh
 * @date 2026/2/5 10:55
 * @description: 容器统计mapper
 */
@Mapper
public interface DockerStatsMapper extends BaseQueryMapper<DockerStats, DockerStats> {

    /**
     * 清理指定时间之前的历史数据
     */
    @Delete({" delete from docker_stats where create_time <= #{createTime} "})
    int deleteByDate(@Param("createTime") String createTime);
}
