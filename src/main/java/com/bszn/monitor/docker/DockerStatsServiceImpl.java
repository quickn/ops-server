package com.bszn.monitor.docker;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author wzh
 * @date 2026/2/5 10:56
 * @description: 容器统计业务层
 */
@Service
public class DockerStatsServiceImpl extends ServiceImpl<DockerStatsMapper, DockerStats> implements IDockerStatsService {
}
