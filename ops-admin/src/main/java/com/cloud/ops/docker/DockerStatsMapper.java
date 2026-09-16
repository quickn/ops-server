package com.cloud.ops.docker;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author wzh
 * @date 2026/2/5 10:55
 * @description: 容器统计mapper
 */
@Mapper
public interface DockerStatsMapper extends BaseMapper<DockerStats> {
}
