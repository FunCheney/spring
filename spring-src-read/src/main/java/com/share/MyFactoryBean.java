package com.share;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author: Fchen
 * @date: 2020/8/10 10:40 下午
 * @desc: TODO
 */
@Component("myFactoryBean")
public class MyFactoryBean implements FactoryBean<MyTest> {

	public void testBean(){
		System.out.println("MyFactoryBean");
	}
	@Override
	public MyTest getObject() throws Exception {
		return new MyTest();
	}

	@Override
	public Class<?> getObjectType() {
		return MyTest.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
