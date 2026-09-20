package com.cloud.ops.system;

import com.cloud.ops.base.MonitorBaseEntity;
import lombok.Data;


@Data
public class NetIoState extends MonitorBaseEntity {


    /**
     *
     */
    private static final long serialVersionUID = -8314012397341825158L;


    /**
     * host名称
     */
    private String hostname;

    /**
     * 每秒钟接收的数据包,rxpck/s
     */
    private String rxpck;

    /**
     * 每秒钟发送的数据包,txpck/s
     */
    private String txpck;


    /**
     * 每秒钟接收的KB数,rxkB/s
     */
    private String rxbyt;


    /**
     * 每秒钟发送的KB数,txkB/s
     */
    private String txbyt;


    /**
     * 每秒钟接收的压缩数据包,rxcmp/s
     */
    private String rxcmp;


    /**
     * 每秒钟发送的压缩数据包,txcmp/s
     */
    private String txcmp;


    /**
     * 每秒钟接收的多播数据包,rxmcst/s
     */
    private String rxmcst;

}
