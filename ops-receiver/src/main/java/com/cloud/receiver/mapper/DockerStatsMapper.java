package com.cloud.receiver.mapper;

import com.cloud.receiver.entity.DockerStats;
import com.cloud.receiver.sql.BaseQueryMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DockerStatsMapper extends BaseQueryMapper<DockerStats, DockerStats> {

    @Delete({" delete from docker_stats where create_time <= #{createTime} "})
    int deleteByDate(String createTime);
}
