package com.fanc.test;

import com.fanc.app.AppConfig;
import com.fanc.dao.TestDao;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author: Fchen
 * @date: 2019-12-22 11:34
 * @desc: TODO
 */
public class MyTest {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(AppConfig.class);
		TestDao dao = applicationContext.getBean(TestDao.class);
		dao.test();
	}

}
