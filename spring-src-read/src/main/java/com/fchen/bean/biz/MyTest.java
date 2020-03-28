package com.fchen.bean.biz;

import com.fchen.config.MyConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author: Fchen
 * @date: 2020/3/28 3:57 下午
 * @desc: TODO
 */
public class MyTest {
	public static void main(String[] args) {
		ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		// 方式一
		FactoryBeanTest beanTest = ann.getBean(FactoryBeanTest.class);
		System.out.println(beanTest);
		// 方式二
		Object user = ann.getBean("factoryBeanTest");
		System.out.println(user);
		// 方式三
		Object user2 = ann.getBean("&factoryBeanTest");
		System.out.println(user2);
	}
}
