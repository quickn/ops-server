package com.bszn.monitor.processnetstat;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 进程统计 Service 实现
 *
 * @author Liuyun
 */
@Service
@Slf4j
public class ProcessStatServiceImpl extends ServiceImpl<ProcessStatMapper, ProcessStat> implements ProcessStatService {

}
