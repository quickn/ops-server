package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface JarDeployRecordMapper extends BaseMapper<JarDeployRecord> {
    
    @Select("SELECT * FROM jar_deploy_record WHERE jar_package_id = #{jarPackageId} ORDER BY create_time DESC")
    List<JarDeployRecord> selectByJarPackageId(@Param("jarPackageId") Integer jarPackageId);
    
    @Select("SELECT * FROM jar_deploy_record WHERE agent_id = #{agentId} ORDER BY create_time DESC LIMIT #{limit}")
    List<JarDeployRecord> selectByAgentId(@Param("agentId") Integer agentId, @Param("limit") Integer limit);
    
    @Select("SELECT COUNT(*) FROM jar_deploy_record WHERE jar_package_id = #{jarPackageId} AND status = #{status}")
    Integer countByStatus(@Param("jarPackageId") Integer jarPackageId, @Param("status") Integer status);
}