package com.cloud.receiver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.DiskState;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiskStateMapper extends BaseMapper<DiskState> {
    @Delete({" delete from disk_state where create_time <= #{createTime} "})
    void deleteByDate(String createTime);
}
