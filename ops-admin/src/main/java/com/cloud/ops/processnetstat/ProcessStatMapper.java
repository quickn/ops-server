package com.cloud.ops.processnetstat;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进程统计 Mapper
 *
 * @author Liuyun
 */
@Mapper
public interface ProcessStatMapper extends BaseQueryMapper<ProcessStat, ProcessStat> {

}
