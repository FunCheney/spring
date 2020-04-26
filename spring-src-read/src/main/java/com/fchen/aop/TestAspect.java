package com.fchen.aop;

import com.fchen.service.MyService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author: Fchen
 * @date: 2020/4/26 10:39 下午
 * @desc: TODO
 */
@Aspect
public class TestAspect {
	@Autowired
	MyService myService;
	@Pointcut("execution(public * com.fchen.service.MyService.test())")
	public void matchCondition() {}

	@Before("matchCondition()")
	public void before() {
		System.out.println("before 前置通知......");
		myService.test();
	}


}
