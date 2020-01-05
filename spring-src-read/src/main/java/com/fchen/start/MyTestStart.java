package com.fchen.start;

import com.fchen.config.MyConfig;
import com.fchen.service.MyService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author: Fchen
 * @date: 2020/1/5 9:24 上午
 * @desc: 启动类
 */
public class MyTestStart {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		MyService myService = ann.getBean(MyService.class);
		myService.test();
	}
}
