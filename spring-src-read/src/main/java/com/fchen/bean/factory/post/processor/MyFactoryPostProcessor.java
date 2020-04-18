package com.fchen.bean.factory.post.processor;

import com.fchen.service.MyService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author: Fchen
 * @date: 2020/3/1 5:29 下午
 * @desc: 自定义 MyFactoryPostProcessor 的使用
 */
@Component
public class MyFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		int count = beanFactory.getBeanDefinitionCount();
		String[] names = beanFactory.getBeanDefinitionNames();
		System.out.println("当前BeanFactory中有"+count+" 个Bean");
		System.out.println(Arrays.asList(names));
		MyService bean = beanFactory.getBean(MyService.class);
		System.out.println(bean);
	}
}
