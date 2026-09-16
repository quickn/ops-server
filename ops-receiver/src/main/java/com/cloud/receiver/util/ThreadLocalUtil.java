package com.cloud.receiver.util;

/**
 * Created by Liuyun on 2023-08-01 14:19
 **/
public class ThreadLocalUtil {

    private static ThreadLocal<Integer> serviceIdLocal = new ThreadLocal();

    public static Integer getServiceId() {
        return serviceIdLocal.get();
    }

    public static void setServiceId(Integer serviceId) {
        serviceIdLocal.set(serviceId);
    }

    public static void removeServiceId() {
        serviceIdLocal.remove();
    }
}
