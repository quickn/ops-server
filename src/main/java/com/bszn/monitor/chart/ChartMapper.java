package com.bszn.monitor.chart;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author wzh
 * @date 2026/1/13 14:10
 * @description: 图表mapper
 */
@Mapper
public interface ChartMapper {

    List<ChartVO> chartList(@Param("chartDto") ChartDto chartDto, @Param("tableName") String tableName, @Param("valueColumn") String valueColumn);

}
