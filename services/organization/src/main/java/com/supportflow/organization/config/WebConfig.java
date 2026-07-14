package com.supportflow.organization.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OrgContextInterceptor orgContextInterceptor;

    public WebConfig(OrgContextInterceptor orgContextInterceptor) {
        this.orgContextInterceptor = orgContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(orgContextInterceptor);
    }
}