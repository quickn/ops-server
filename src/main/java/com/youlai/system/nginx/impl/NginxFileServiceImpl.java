package com.youlai.system.nginx.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.system.mapper.NginxFileMapper;
import com.youlai.system.nginx.NginxFile;
import com.youlai.system.nginx.NginxFileQuery;
import com.youlai.system.nginx.NginxFileService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Liuyun on 2023-09-06 14:49
 **/
@Service
public class NginxFileServiceImpl extends ServiceImpl<NginxFileMapper, NginxFile> implements NginxFileService {
    @Override
    public List<NginxFile> listFiles(NginxFileQuery nginxFileQuery) {
        LambdaQueryWrapper lambdaQueryWrapper = new LambdaQueryWrapper();
        return baseMapper.selectList(lambdaQueryWrapper);
    }
}
