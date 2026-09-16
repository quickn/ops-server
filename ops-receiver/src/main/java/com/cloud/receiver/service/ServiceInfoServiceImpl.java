package com.cloud.receiver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.receiver.entity.ServiceInfo;
import com.cloud.receiver.mapper.ServiceInfoMapper;
import org.springframework.stereotype.Service;

/**
 * Created by Liuyun on 2023-08-26 10:29
 **/
@Service
public class ServiceInfoServiceImpl extends ServiceImpl<ServiceInfoMapper,
        ServiceInfo> implements IService<ServiceInfo> {
}
