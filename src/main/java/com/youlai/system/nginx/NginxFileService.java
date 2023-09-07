package com.youlai.system.nginx;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Created by Liuyun on 2023-09-06 14:47
 **/
public interface NginxFileService extends IService<NginxFile> {
    List<NginxFile> listFiles(NginxFileQuery nginxFileQuery);
}
