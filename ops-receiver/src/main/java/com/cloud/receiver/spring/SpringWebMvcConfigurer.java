package com.cloud.receiver.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "web")
public class SpringWebMvcConfigurer implements WebMvcConfigurer {

    private String rootPath;
    private String pathPatterns;
    private List<String> excludePathPatterns;

    /**
     * 添加静态资源--过滤swagger-api (开源的在线API文档)
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //和页面有关的静态目录都放在项目的static目录下
        // registry.addResourceHandler("/doc.html").addResourceLocations("classpath*:/META-INF/resources/");
        // registry.addResourceHandler("/webjars/**").addResourceLocations("classpath*:/META-INF/resources/webjars/");

        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/temp/**").addResourceLocations("classpath:/templates/");

        //  registry.addResourceHandler("/images/**").addResourceLocations("file:" + excelPath);
        //上传的图片在D盘下的OTA目录下，访问路径如：http://localhost:8081/OTA/d3cf0281-bb7f-40e0-ab77-406db95ccf2c.jpg
        //其中OTA表示访问的前缀。"file:D:/OTA/"是文件真实的存储路径
        if (StringUtils.isNotEmpty(rootPath)) {
            registry.addResourceHandler(pathPatterns).addResourceLocations("file:" + rootPath);
            //registry.addResourceHandler("/car-snap/**").addResourceLocations("file:d://image//car-snap/");
        } else {
            registry.addResourceHandler("/upload/**").addResourceLocations("file:/home/web/upload/");
        }
        //swagger
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(logInterceptor());
        String[] excludePath = {"/*.html",
                "/favicon.ico",
                "/**/*.html",
                "/**/*.css",
                "/**/*.js",
                "/**/*.png",
                "/**/*.jpg",
                "/**/*.apk",
                "/swagger-resources/**",
                "/v3/api-docs/**",
                "/v2/api-docs/**",
                "/**/macs/**"};
        // InterceptorRegistration interceptorRegistration = registry.addInterceptor(authInterceptor).addPathPatterns("/**")
        //   .excludePathPatterns(excludePath);
        //if (excludePathPatterns != null)
        //   interceptorRegistration.excludePathPatterns(excludePathPatterns);
        // registry.addInterceptors(registry);
    }

    //解决后台传long类型精度丢失问题
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = null;
        // JSON 转换器在默认列表中的原始下标，处理完后必须原样插回，不能追加到列表末尾
        int jacksonIndex = -1;

        for (int i = converters.size() - 1; i >= 0; i--) {
            HttpMessageConverter<?> messageConverter = converters.get(i);
            if (messageConverter instanceof MappingJackson2CborHttpMessageConverter) {
                converters.remove(i);
                continue;
            }
            if (messageConverter instanceof MappingJackson2HttpMessageConverter) {
                jackson2HttpMessageConverter = (MappingJackson2HttpMessageConverter) messageConverter;
                jacksonIndex = i;
                converters.remove(i);
            }
        }
        if (jackson2HttpMessageConverter == null)
            jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();

        ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
        /**
         * 序列换成json时,将所有的long变成string
         * 因为js中得数字类型不能包含所有的java long值
         */
        SimpleModule simpleModule = new SimpleModule();
        // simpleModule.addSerializer(Long.class, LongToLongConverter.instance);
        // simpleModule.addSerializer(Long.TYPE, LongToLongConverter.instance);
        //simpleModule.addSerializer(Instant.class, LongToLongConverter.instance);

        objectMapper.registerModule(simpleModule);

        // 指定json转换时间类型的时区
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        // 指定返回的时间格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);
        /*
         * 必须插回 JSON 转换器的原始位置（在 YAML 转换器之前），不能直接 add 到末尾。
         * 原因：Spring Framework 6.2 起默认注册了 MappingJackson2YamlHttpMessageConverter
         * （位于默认列表最后，且能处理）。若把 JSON 转换器挪到它后面，客户端未显式指定 Accept（如 agent 发送 Accept）时内容协商会优先选中 YAML，
         * 于是 /receiver/agent/getConf 等接口返回 application/yaml 而不是 JSON。
         */
        if (jacksonIndex >= 0) {
            converters.add(jacksonIndex, jackson2HttpMessageConverter);
        } else {
            converters.add(jackson2HttpMessageConverter);
        }
    }


    // 设置跨域访问
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedHeaders("*")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE")
                .allowCredentials(false);
    }


}
