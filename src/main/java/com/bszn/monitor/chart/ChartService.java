package com.bszn.monitor.chart;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wzh
 * @date 2026/1/13 14:11
 * @description: 图表业务层
 */
@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChartMapper chartMapper;

    public List<ChartVO> cpuList(ChartDto chartDto) {
        return chartList(chartDto, "cpu_state", "SYS");
    }

    public List<ChartVO> memList(ChartDto chartDto) {
        return chartList(chartDto, "mem_state", "USE_PER");
    }

    public List<ChartVO> chartList(ChartDto chartDto, String tableName, String valueColumn) {
        return chartMapper.chartList(chartDto, tableName, valueColumn);
    }
}
