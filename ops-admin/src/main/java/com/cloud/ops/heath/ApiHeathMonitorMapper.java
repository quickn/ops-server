package com.cloud.ops.heath;

import com.cloud.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApiHeathMonitorMapper extends BaseQueryMapper<ApiHeathMonitor, ApiHeathMonitor> {

    public List<ApiHeathMonitor> selectAllByParams(Map<String, Object> map) throws Exception;

    public ApiHeathMonitor selectById(String id) throws Exception;

    public int deleteById(String[] id) throws Exception;

    public int countByParams(Map<String, Object> params) throws Exception;

    public void updateList(List<ApiHeathMonitor> recordList) throws Exception;

    @Select(" select * from api_heath_monitor where is_monitor = 1 ")
    List<ApiHeathMonitor> selectListByMonitor();
}
