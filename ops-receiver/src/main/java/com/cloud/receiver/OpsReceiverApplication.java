package com.cloud.receiver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

/**
 * 接收服务启动类。
 *
 * <p>该模块支持两种运行方式：
 * <ol>
 *     <li>独立启动：java -jar ops-receiver-exec.jar</li>
 *     <li>作为依赖被 ops-admin 引入，随 ops-admin 一起启动（组件扫描会加载 com.cloud.receiver 下的 Bean）</li>
 * </ol>
 * 因此这里的 {@code @Bean} 均使用 {@link ConditionalOnMissingBean}，
 * 避免与宿主（ops-admin）中同类型 Bean（如 ApiHeathConfig#restTemplate）冲突。
 */
@SpringBootApplication
@MapperScan("com.cloud.receiver.mapper")  // 确保路径正确
@EnableCaching
@EnableScheduling
public class OpsReceiverApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsReceiverApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        RestTemplate restTemplate = new RestTemplateBuilder().additionalMessageConverters(m).build();
        return restTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(50);
        return taskScheduler;
    }

}
