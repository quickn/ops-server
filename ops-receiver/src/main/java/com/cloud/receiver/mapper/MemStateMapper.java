package com.cloud.receiver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.MemState;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface MemStateMapper extends BaseMapper<MemState> {
    
    @Delete({" delete from mem_state where create_time <= #{createTime} "})
    int deleteByDate(String createTime);

}
