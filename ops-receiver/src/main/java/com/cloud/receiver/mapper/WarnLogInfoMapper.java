package com.cloud.receiver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.WarnLogInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WarnLogInfoMapper extends BaseMapper<WarnLogInfo> {

    @Delete({" delete from warn_log_info where create_time <= #{createTime} "})
    int deleteByDate(String createTime);

    @Select(" select * from warn_log_info where hostname=#{hostname} and title=#{title} and send_email=1 order by id desc limit 1 ")
    WarnLogInfo getLastByHostnameAndTitle(@Param("hostname") String hostname, @Param("title") String title);
}
