package com.fchen.aware;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/8/22 3:51 下午
 * @desc: TODO
 */
@Service
@Scope("prototype")
public class Test {

	@Autowired
	ApplicationContext applicationContext;

	public void test(){
		System.out.println("Test ->" + applicationContext.getBean("test"));
	}
}
