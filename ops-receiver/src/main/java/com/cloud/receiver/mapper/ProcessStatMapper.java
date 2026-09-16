package com.cloud.receiver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.ProcessStat;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessStatMapper extends BaseMapper<ProcessStat> {

    @Delete({" delete from process_stat where create_time <= #{createTime} "})
    int deleteByDate(String createTime);
}
