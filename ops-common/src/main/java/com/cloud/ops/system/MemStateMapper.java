package com.cloud.ops.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemStateMapper extends BaseMapper<MemState> {

    /**
     * 清理指定时间之前的历史数据
     */
    @Delete({" delete from mem_state where create_time <= #{createTime} "})
    int deleteByDate(@Param("createTime") String createTime);

}
