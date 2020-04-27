package com.fchen.aop;

import com.fchen.service.MyService;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: Fchen
 * @date: 2020/4/26 10:39 下午
 * @desc: TODO
 */
@Component
@Aspect
public class TestAspect {
	@Pointcut("execution(public * com.fchen.service.MyService.test())")
	public void matchCondition() {}

	@Before("matchCondition()")
	public void before() {
		System.out.println("before 前置通知......");
	}

	@After("matchCondition()")
	public void after() {
		System.out.println("after 后置通知......");
	}

}
