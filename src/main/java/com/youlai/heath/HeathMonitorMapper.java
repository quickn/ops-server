package com.youlai.heath;

import com.youlai.base.mapper.BaseQueryMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HeathMonitorMapper extends BaseQueryMapper<HeathMonitor, HeathMonitor> {

    public List<HeathMonitor> selectAllByParams(Map<String, Object> map) throws Exception;

    public HeathMonitor selectById(String id) throws Exception;

    public int deleteById(String[] id) throws Exception;

    public int countByParams(Map<String, Object> params) throws Exception;

    public void updateList(List<HeathMonitor> recordList) throws Exception;

}
