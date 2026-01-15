package com.bszn.monitor.project;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectDeployRecordMapper extends BaseMapper<ProjectDeployRecord> {

    @Select("SELECT * FROM project_deploy_record WHERE project_id = #{projectId} ORDER BY create_time DESC")
    List<ProjectDeployRecord> selectByProjectId(@Param("projectId") Long projectId);
}