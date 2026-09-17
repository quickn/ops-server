package com.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ops-admin 启动类。
 * <p>
 * 注意：这里<b>不要</b>再声明 {@code @MapperScan}。
 * Mapper 扫描的唯一来源是 {@link com.cloud.receiver.OpsReceiverApplication}
 * （{@code basePackages = {"com.cloud.ops", "com.cloud.system.mapper"}, annotationClass = Mapper.class}）。
 * ops-admin 依赖 ops-receiver 的非 exec jar，组件扫描会把该类作为配置类加载，扫描同样生效。
 * <p>
 * 历史问题：此处曾写为
 * {@code @MapperScan(basePackages = {"com.baomidou.mybatisplus.core.mapper", "com.cloud.ops.mapper", "com.cloud.ops.*"})}，
 * 由于未设置 {@code annotationClass}，{@code com.cloud.ops.*} 会把包内<b>所有接口</b>注册成
 * {@code MapperFactoryBean}，例如 Service 接口 {@code com.cloud.ops.msg.IMsgService}，
 * 从而与真正的实现类 {@code CmdMsgServiceImpl} 一起构成同类型的两个候选 Bean，
 * 报错：A component required a single bean, but 2 were found: cmdMsgServiceImpl / IMsgService。
 */
@SpringBootApplication
@EnableAsync
public class OpsAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsAdminApplication.class, args);
    }
}
