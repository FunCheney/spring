package com.demo.factory;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author: Fchen
 * @date: 2020/7/8 8:33 下午
 * @desc: TODO
 */
public class FactoryBeanDemoOne implements FactoryBean<BeanDemoOne> {
	@Override
	public BeanDemoOne getObject() throws Exception {
		return new BeanDemoOne();
	}

	@Override
	public Class<?> getObjectType() {
		return null;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
	public FactoryBeanDemoOne (){}

	public FactoryBeanDemoOne (int i){

	}
}
