package com.cloud.receiver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.CpuState;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CpuStateMapper extends BaseMapper<CpuState> {

    @Delete({" delete from cpu_state where create_time <= #{createTime} "})
    int deleteByDate(String createTime);
}
