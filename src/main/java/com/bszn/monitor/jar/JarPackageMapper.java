package com.bszn.monitor.jar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface JarPackageMapper extends BaseMapper<JarPackage> {

    @Select("SELECT * FROM jar_package WHERE file_name = #{fileName} ORDER BY create_time DESC")
    List<JarPackage> selectByFileName(@Param("fileName") String fileName);

    @Select("SELECT MAX(version) FROM jar_package WHERE file_name = #{fileName}")
    String selectMaxVersion(@Param("fileName") String fileName);
}