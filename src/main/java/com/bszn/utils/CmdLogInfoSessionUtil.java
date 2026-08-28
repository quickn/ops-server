package com.bszn.utils;


import com.alibaba.ttl.TransmittableThreadLocal;
import com.bszn.monitor.cmdlog.CmdLogInfo;

/**
 * Created by Liuyun on 2022-06-10 9:37
 **/
public class CmdLogInfoSessionUtil {

    private static final TransmittableThreadLocal<CmdLogInfo> session = new TransmittableThreadLocal<CmdLogInfo>();


    public static CmdLogInfo get() {
        return session.get();
    }

    public static void set(CmdLogInfo cmdLogInfo) {
        session.set(cmdLogInfo);
    }

    public static void remove() {
        session.remove();
    }
}
