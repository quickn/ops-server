package com.youlai.log;

import com.youlai.base.ServiceBaseEntity;
import lombok.Data;

/**
 * @version v2.3
 * @ClassName:LogInfo.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: 日志信息
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
@Data
public class LogInfo extends ServiceBaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = 1565538727002722890L;

    /**
     * host名称
     */
    private String hostname;

    private String title;

    /**
     * 描述
     */
    private String infoContent;

    /**
     * 0成功，1失败
     */
    private String state;

    private Boolean sendEmail;

}
