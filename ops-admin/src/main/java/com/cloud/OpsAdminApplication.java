package com.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
//排除自动加载的配置
//@EnableAutoConfiguration()
public class OpsAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsAdminApplication.class, args);
    }
}
