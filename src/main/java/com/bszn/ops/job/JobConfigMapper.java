package com.bszn.ops.job;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobConfigMapper extends BaseMapper<JobConfig> {
    /**
     * 根据jobId查询
     *
     * @param jobId
     * @return
     */
    @Select("select * from job_config where job_id = #{jobId}")
    JobConfig selectByJobId(Integer jobId);
}
