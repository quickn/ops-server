package com.cloud.receiver.util;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class RestUtil {

    @Resource
    private RestTemplate restTemplate;

    public int get(String url) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
            return responseEntity.getStatusCodeValue();
        } catch (HttpClientErrorException e) {
            log.error("服务接口检测任务错误", e);
            return e.getRawStatusCode();
        } catch (Exception e) {
            log.error("服务接口检测任务错误", e);
            return 500;
        }
    }


}
