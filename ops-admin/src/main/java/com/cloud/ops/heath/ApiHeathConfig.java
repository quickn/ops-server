package com.cloud.ops.heath;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.Duration;

@Component
@Order(value = 1)
public class ApiHeathConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        StringHttpMessageConverter m = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        restTemplateBuilder.additionalMessageConverters(m);
        restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(5)) // 连接超时：5秒
                .setReadTimeout(Duration.ofSeconds(10));       // 读取超时：10秒
        return restTemplateBuilder.build();
    }

}