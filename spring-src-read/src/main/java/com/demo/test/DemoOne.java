package com.demo.test;

import com.demo.config.DemoConfigOne;
import com.fchen.service.MyService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author: Fchen
 * @date: 2020/7/7 11:53 下午
 * @desc: TODO
 */
public class DemoOne {
	public static void main(String[] args) {

		ApplicationContext ann = new AnnotationConfigApplicationContext(DemoConfigOne.class);
	}
}
