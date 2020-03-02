package com.fchen.bean.factory.post.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author: Fchen
 * @date: 2020/3/2 3:39 下午
 * @desc: 自定义且交给Spring管理的 TestFactoryPostProcessor
 *        通过 {@link Component} 来交给Spring管理
 */

//public class TestFactoryPostProcessor implements BeanFactoryPostProcessor {
//
//
//	@Override
//	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//		int count = beanFactory.getBeanDefinitionCount();
//		String[] names = beanFactory.getBeanDefinitionNames();
//		System.out.println("当前BeanFactory中有"+count+" 个Bean");
//		System.out.println(Arrays.asList(names));
//	}
//}