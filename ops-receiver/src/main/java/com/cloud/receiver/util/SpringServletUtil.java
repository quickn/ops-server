package com.cloud.receiver.util;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Enumeration;

/**
 * Created by Liuyun on 2021-11-10 11:05
 *
 * @author liuyun
 */
public class SpringServletUtil {

    public static HttpServletRequest getHttpServletRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        if (request != null) {
            return request;
        }
        return null;
    }

    public static JSONObject getParameters(HttpServletRequest httpServletRequest) {
        JSONObject hookMap = new JSONObject();
        Enumeration<?> temp = httpServletRequest.getParameterNames();
        if (null != temp) {
            while (temp.hasMoreElements()) {
                String en = (String) temp.nextElement();
                String value = httpServletRequest.getParameter(en);
                hookMap.put(en, value);
            }
        }
        return hookMap;
    }
}
