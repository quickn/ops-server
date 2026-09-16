package com.cloud.base.http;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class RestUtil {

    @Autowired
    private RestTemplate restTemplate;

    public JSONObject post(String url, JSONObject jsonObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString());
        HttpEntity<String> httpEntity = new HttpEntity<>(JSONUtil.parse(jsonObject).toString(), headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
        return JSONUtil.parseObj(responseEntity.getBody());
    }

    public JSONObject post(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString());
        HttpEntity<String> httpEntity = new HttpEntity<>("", headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
        return JSONUtil.parseObj(responseEntity.getBody());
    }

    public ResponseEntity<String> post(String url, String contentType, String param) {
        HttpHeaders headers = new HttpHeaders();
        if ("x-www-form-urlencoded".equals(contentType)) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        }
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString());
        HttpEntity<String> httpEntity = new HttpEntity<>(param, headers);
        return restTemplate.postForEntity(url, httpEntity, String.class);
    }

    public int get(String url) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url.trim(), String.class);
            return responseEntity.getStatusCode().value();
        } catch (HttpClientErrorException e) {
            log.error("服务接口检测任务错误", e);
            return e.getRawStatusCode();
        } catch (Exception e) {
            log.error("服务接口检测任务错误", e);
            return 500;
        }
    }

}
