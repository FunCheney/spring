package com.fchen.expand;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Map;

/**
 * @author: Fchen
 * @date: 2020/5/12 10:48 下午
 * @desc: ImportBeanDefinitionRegistrar 的应用
 */
public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		GenericBeanDefinition demoBd = (GenericBeanDefinition) registry.getBeanDefinition("demoServiceOne");
		demoBd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
		registry.registerBeanDefinition("demoServiceOne",demoBd);

		Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(MyScan.class.getName());
		String[] basePackages = (String[]) annotationAttributes.get("basePackages");
		if (basePackages == null || basePackages.length == 0) {
			String basePackage = null;
			try {
				basePackage = Class.forName(importingClassMetadata.getClassName()).getPackage().getName();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			basePackages = new String[] {basePackage};
		}

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
        // 这一行必须要加
		scanner.addIncludeFilter(new AnnotationTypeFilter(MyScan.class));
		scanner.scan(basePackages);
	}
}
