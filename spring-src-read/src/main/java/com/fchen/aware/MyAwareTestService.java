package com.fchen.aware;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/8/22 3:52 下午
 * @desc: TODO
 */
@Service("myAwareTestService")
public class MyAwareTestService {
	@Autowired
	Test test;
	@Autowired
	TestAware1 testAware1;
	@Autowired
	Test1 test1;


	public void myTest(){
//		testAware1.test();
//		test.test();
		test1.test();
	}
}
