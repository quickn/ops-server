package com.cloud.ops.warnLog;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Created by Liuyun on 2023-09-09 16:51
 **/
@Mapper
public interface WarnLogInfoMapper extends BaseQueryMapper<WarnLogInfo, WarnLogInfo> {
    @Select(" select * from warn_log_info where hostname=#{hostname} and title=#{title} and send_email=1 order by id desc limit 1 ")
    WarnLogInfo getLastByHostnameAndTitle(@Param("hostname") String hostname, @Param("title") String title);

    @Select(" select * from warn_log_info where service_id=#{serviceId} and title=#{title} and send_email=1 order by id desc limit 1 ")
    WarnLogInfo getLastByServiceIdAndTitle(@Param("serviceId") Integer serviceId, @Param("title") String title);

    /**
     * 清理指定时间之前的历史数据
     */
    @Delete({" delete from warn_log_info where create_time <= #{createTime} "})
    int deleteByDate(@Param("createTime") String createTime);
}
