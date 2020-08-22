package com.fchen.start;

import com.fchen.aware.MyAwareTestService;
import com.fchen.bean.factory.post.processor.MyFactoryPostProcessor;
import com.fchen.config.MyConfig;
//import com.fchen.config.MySelectorImportConfig;
//import com.fchen.expand.MyImportSelector;
//import com.fchen.service.MyScannerService;
//import com.fchen.service.MySelectImportService;
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

		// todo 使用方式一
//		ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
//		MyService myService = ann.getBean(MyService.class);
//		MyService myService = (MyService)ann.getBean("myService");
//		myService.test();
		// todo 使用方式二
//		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
//		ctx.register(MyConfig.class);

		// todo 扫描器区分
//		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//		context.scan("com.fchen");

//		MyScannerService myScannerService = ann.getBean(MyScannerService.class);
//		myScannerService.testMyScan();

		// todo 使用方式三
//		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
//		ctx.register(MyConfig.class);
//		ctx.scan("com.fchen");
//		ctx.refresh();
//		MyService myService = ctx.getBean(MyService.class);
//		myService.test();


//		//手动添加程序员自定义 且 未交给spring 管理的 BeanFactoryPostProcessor
//		ctx.addBeanFactoryPostProcessor(new MyFactoryPostProcessor());
//		ctx.refresh();
//		MyService myService1 = ctx.getBean(MyService.class);
//		ApplicationContext ann = new AnnotationConfigApplicationContext(MySelectorImportConfig.class);
//		MySelectImportService bean = ann.getBean(MySelectImportService.class);
//		bean.test();

		// todo Aware 使用测试
		ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		MyAwareTestService service = (MyAwareTestService) ann.getBean("myAwareTestService");
		service.myTest();
		service.myTest();


	}
}
