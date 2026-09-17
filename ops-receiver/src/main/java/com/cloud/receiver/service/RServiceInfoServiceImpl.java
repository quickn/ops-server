package com.cloud.receiver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.ops.service.ServiceInfo;
import com.cloud.ops.service.ServiceInfoMapper;
import org.springframework.stereotype.Service;

/**
 * Created by Liuyun on 2023-08-26 10:29
 **/
@Service
public class RServiceInfoServiceImpl extends ServiceImpl<ServiceInfoMapper,
        ServiceInfo> implements IService<ServiceInfo> {
}
