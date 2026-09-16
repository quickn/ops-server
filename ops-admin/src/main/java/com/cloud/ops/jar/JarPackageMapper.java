package com.cloud.ops.jar;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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

    /**
     * 按文件名分组取最大版本号数据（分页）
     */
    @Select("SELECT jp1.* " +
            "FROM jar_package jp1 " +
            "INNER JOIN ( " +
            "    SELECT file_name, MAX(version) as max_version " +
            "    FROM jar_package " +
            "    GROUP BY file_name " +
            ") jp2 ON jp1.file_name = jp2.file_name AND jp1.version = jp2.max_version " +
            "${ew.customSqlSegment} " +
            "ORDER BY jp1.file_name, jp1.version DESC")
    IPage<JarPackage> selectLatestVersionByPage(IPage<JarPackage> page, @Param("ew") Wrapper<JarPackage> queryWrapper);

}