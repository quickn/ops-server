package com.cloud.ops.system;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface DiskStateMapper extends BaseQueryMapper<DiskState, DiskState> {
}
