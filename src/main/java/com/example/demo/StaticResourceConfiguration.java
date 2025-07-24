package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置图片资源访问路径
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:D:/软件/编程/Code/后台/server/images/");

        // 配置视频资源访问路径
        registry.addResourceHandler("/videos/**")
                .addResourceLocations("file:D:/软件/编程/Code/后台/server/videos/");
    }
}
