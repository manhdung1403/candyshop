package com.finalproject.candyshop.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Trỏ tới src/main/resources/static/uploads/ theo đường dẫn tuyệt đối
        String uploadPath = Paths.get("src/main/resources/static/uploads/")
                .toAbsolutePath().toString() + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}