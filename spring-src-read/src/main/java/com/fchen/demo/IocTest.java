package com.fchen.demo;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * @author: Fchen
 * @date: 2020/1/8 8:28 上午
 * @desc: 编程式使用IOC容器
 */
public class IocTest {
	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("spring-bean.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);
	}
}
