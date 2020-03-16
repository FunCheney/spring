package com.fchen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/1/5 9:23 上午
 * @desc: spring 测试类
 */
@Service
public class MyService {

	private Logger log = LoggerFactory.getLogger(MyService.class);

	public void test(){
		System.out.println("hello test");
	}
}
