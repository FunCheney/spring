package com.share;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author: Fchen
 * @date: 2020/8/10 10:44 下午
 * @desc: TODO
 */
public class ShareTest {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ann = new AnnotationConfigApplicationContext(Config.class);
//		MyFactoryBean myFactoryBean = (MyFactoryBean) ann.getBean("myFactoryBean");
//		myFactoryBean.testBean();

		MyTest bean = (MyTest)ann.getBean("myFactoryBean");
		bean.myTest();

		MyFactoryBean myFactoryBean = (MyFactoryBean) ann.getBean("&myFactoryBean");
		myFactoryBean.testBean();

	}
}
