package com.fchen.demo;

import com.fchen.service.ClassPathAppContextService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author: Fchen
 * @date: 2020/2/18 10:52 下午
 * @desc: ClassPathApplicationContext
 */
public class ClassPathApplicationContextTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext xmlAppContext = new ClassPathXmlApplicationContext("spring-bean.xml");
		ClassPathAppContextService service = (ClassPathAppContextService)xmlAppContext.getBean("classPathAppContextService");
		service.test();
	}
}
