package com.fchen.start;

import com.fchen.bean.factory.post.processor.MyFactoryPostProcessor;
import com.fchen.config.MyConfig;
import com.fchen.service.MyService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author: Fchen
 * @date: 2020/1/5 9:24 上午
 * @desc: 启动类
 */
public class MyTestStart {
	public static void main(String[] args) {

		ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		MyService myService = ann.getBean(MyService.class);
		myService.test();

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MyConfig.class);
		//手动添加程序员自定义 且 未交给spring 管理的 BeanFactoryPostProcessor
		ctx.addBeanFactoryPostProcessor(new MyFactoryPostProcessor());
		ctx.refresh();
		MyService myService1 = ctx.getBean(MyService.class);
	}
}
