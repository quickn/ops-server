package com.cloud.receiver;

import org.apache.ibatis.annotations.Mapper;
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
 *
 * <h3>关于 {@code @MapperScan} 的位置</h3>
 * 放在本类而非 {@code OpsAdminApplication}，因为本类是两种运行方式下<b>唯一必然被加载</b>的入口：
 * <ul>
 *     <li>ops-admin 启动时，其组件扫描范围 {@code com.cloud} 会把本类作为 {@code @Configuration} 加载；</li>
 *     <li>receiver 独立启动时，本类是程序入口，自然被加载。</li>
 * </ul>
 * 这样既保留单一来源，又避免在 ops-admin 上多写一份（双写会导致两个 {@code MapperScannerConfigurer}
 * 同时注册并重叠扫描同一类路径）。
 *
 * <h3>关于 {@code annotationClass = Mapper.class}</h3>
 * <b>这是上一轮报错的修复点。</b>
 * MyBatis-Plus 3.5.7 的 {@code AutoConfiguredMapperScannerRegistrar} 默认<b>不会</b>设置
 * {@code annotationClass}，{@code ClassPathMapperScanner} 在没有注解过滤器的情况下会把包内
 * <b>所有接口</b>（包括 Service 接口如 {@code AgentService}）都注册成
 * {@code MapperFactoryBean}，导致注入 {@code AgentService} 时出现
 * 「agentServiceImpl / agentService 2 beans found」歧义。
 * 显式声明 {@code annotationClass = Mapper.class} 后，只会注册带 {@code @Mapper} 注解的接口，
 * Service 接口不再被误注册。
 *
 * <h3>关于 basePackages</h3>
 * 显式列出两个 Mapper 根包，避开 MyBatis-Plus 自动扫描对未知包的全量接口扫描：
 * <ul>
 *     <li>{@code com.cloud.ops}：14 个公共 Mapper（ops-common）；</li>
 *     <li>{@code com.cloud.system.mapper}：8 个系统 Mapper（ops-admin，仅 ops-admin 模式可达）。</li>
 * </ul>
 * receiver 独立启动时 {@code com.cloud.system.mapper} 不在 classpath 上，
 * {@code MapperScannerConfigurer} 对不存在的根包只发 warning、不报错。
 */
@SpringBootApplication
@MapperScan(
        basePackages = {"com.cloud.ops", "com.cloud.system.mapper"},
        annotationClass = Mapper.class)
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
