package com.fchen.aware;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/8/22 3:44 下午
 * @desc: TODO
 */
@Service
@Scope("prototype")
public class TestAware1 implements ApplicationContextAware {
	private ApplicationContext applicationContext;
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void test(){
		// 先去容器中获取
		TestAware1 testAware1 = (TestAware1) applicationContext.getBean("testAware1");
		System.out.println("TestAware1 ->" + testAware1);
	}
}
