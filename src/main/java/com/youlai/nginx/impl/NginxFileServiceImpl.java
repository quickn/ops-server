package com.youlai.nginx.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.nginx.mapper.NginxFileMapper;
import com.youlai.nginx.NginxFile;
import com.youlai.nginx.NginxFileQuery;
import com.youlai.nginx.NginxFileService;
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
