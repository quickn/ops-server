package com.bszn.monitor.chart;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wzh
 * @date 2026/1/13 14:11
 * @description: 图表业务层
 */
@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChartMapper chartMapper;

    public Map<String, List<ChartVO>> cpuList(ChartDto chartDto) {
        return chartList(chartDto, "cpu_state", "SYS");
    }

    public Map<String, List<ChartVO>> memList(ChartDto chartDto) {
        return chartList(chartDto, "mem_state", "USE_PER");
    }

    public Map<String, List<ChartVO>> chartList(ChartDto chartDto, String tableName, String valueColumn) {
        List<ChartBO> chartBOS = chartMapper.chartList(chartDto, tableName, valueColumn);
        if (CollUtil.isEmpty(chartBOS)) {
            return Collections.emptyMap();
        }
        return chartBOS.stream()
                .collect(Collectors.groupingBy(
                        ChartBO::getHostname,  // 按 hostname 分组
                        Collectors.mapping(
                                ChartVO::new,  // 将每个 ChartBO 转换为 ChartVO
                                Collectors.toList()
                        )
                ));
    }
}
