package com.youlai.base.log;

/**
 * Created by Liuyun on 2022-01-05 10:18
 **/

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 耗时统计
 */
@Aspect
@Component
@Slf4j
public class TakeTimeAspect {

    //统计请求的处理时间
    ThreadLocal<Long> startTime = new ThreadLocal<>();

    /**
     * 带有@TakeTime注解的方法
     */
    @Pointcut("@annotation(com.youlai.base.log.TakeTime)")
    public void log() {

    }

    @Before("log()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        startTime.set(System.currentTimeMillis());
        //接收到请求，记录请求内容
        //ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // HttpServletRequest request = attributes.getRequest();
        //记录请求的内容
        //  log.info("请求URL:" + request.getRequestURL().toString());
        //log.info("请求METHOD:" + request.getMethod());
    }

    @AfterReturning(returning = "ret", pointcut = "log()")
    public void doAfterReturning(JoinPoint joinPoint, Object ret) {
        //处理完请求后，返回内容
        // log.info("方法返回值:" + JSON.toJSONString(ret));
        if (startTime.get() == null) {
            return;
        }
        long timeTnterval = System.currentTimeMillis() - startTime.get();
        if (timeTnterval > 1500) {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method method = methodSignature.getMethod();
            log.warn("执行方法 {} 耗时 {}", method.getName(), timeTnterval);
            startTime.remove();
        }
    }

}
