package com.youlai.nginx;

import lombok.Data;

/**
 * @Auther: 郭胜凯
 * @Date: 2019-04-23 09:19
 * @Email 719348277@qq.com
 * @Description: Nginx 配置文本承载类
 */
@Data
public class NginxConf {

    /**
     * 配置文本
     */
    private String conf;
    private String filePath;

}