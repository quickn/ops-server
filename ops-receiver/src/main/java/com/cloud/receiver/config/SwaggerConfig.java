package com.cloud.receiver.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * @author liuyun
 * @create 2022-11-03 11:43
 **/
@ConditionalOnExpression("!'${spring.profiles.active}'.equals('prod') ")
public class SwaggerConfig {

}
