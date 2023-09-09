package com.youlai.log;

import com.youlai.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Created by Liuyun on 2023-09-09 16:51
 **/
@Mapper
public interface LogInfoMapper extends BaseQueryMapper<LogInfo, LogInfo> {
    @Select(" select * from log_info where hostname=#{hostname} and title=#{title} and send_email=1 order by id desc limit 1 ")
    LogInfo getLastByHostnameAndTitle(@Param("hostname") String hostname, @Param("title") String title);
}
