package com.cloud.receiver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.receiver.entity.SysLoadState;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface SysLoadStateMapper extends BaseMapper<SysLoadState> {

    @Delete({" delete from sys_load_state where create_time <= #{createTime} "})
    int deleteByDate(String createTime);

}
