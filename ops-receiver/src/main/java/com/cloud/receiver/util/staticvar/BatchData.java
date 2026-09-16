package com.cloud.receiver.util.staticvar;

import com.cloud.receiver.entity.*;

import java.util.*;


public class BatchData {

    //进程信息
    public static List<AppInfo> APP_INFO_LIST = Collections.synchronizedList(new ArrayList<AppInfo>());

    //进程状态
    public static List<AppState> APP_STATE_LIST = Collections.synchronizedList(new ArrayList<AppState>());

    //cpu监控
    public static List<CpuState> CPU_STATE_LIST = Collections.synchronizedList(new ArrayList<CpuState>());

    //内存监控
    public static List<MemState> MEM_STATE_LIST = Collections.synchronizedList(new ArrayList<MemState>());

    //网络吞吐监控，暂没用
    public static List<NetIoState> NETIO_STATE_LIST = Collections.synchronizedList(new ArrayList<NetIoState>());

    //磁盘大小
    public static Map<String, ArrayList<DiskState>> DESK_STATE_MAP = Collections.synchronizedMap(new HashMap<>());


    //系统负载监控
    public static List<SysLoadState> SYSLOAD_STATE_LIST = Collections.synchronizedList(new ArrayList<SysLoadState>());

    //日志信息
    public static List<WarnLogInfo> LOG_INFO_LIST = Collections.synchronizedList(new ArrayList<WarnLogInfo>());

}
