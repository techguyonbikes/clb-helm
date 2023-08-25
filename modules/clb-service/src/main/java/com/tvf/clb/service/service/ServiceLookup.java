package com.tvf.clb.service.service;


import com.tvf.clb.base.anotation.ClbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ServiceLookup {

    @Autowired
    private ApplicationContext applicationContext;

    public <T> T forBean(Class<T> serviceClass, String componentType) {

        Map<String, T> beans = applicationContext.getBeansOfType(serviceClass);

        return beans.entrySet().stream()
                .filter(entry -> getAnnotatedComponentType(entry.getKey()).equals(componentType))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new NoSuchBeanDefinitionException(serviceClass.getSimpleName(), componentType));

    }

    private String getAnnotatedComponentType(String beanName) {
        ClbService componentType = applicationContext.findAnnotationOnBean(beanName, ClbService.class);
        return componentType == null ? "" : componentType.componentType();
    }

    public <T> T forBean(Class<T> serviceClass) {
        return applicationContext.getBean(serviceClass);
    }
}
