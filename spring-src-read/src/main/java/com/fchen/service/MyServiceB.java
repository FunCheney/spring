package com.fchen.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/7/1 10:30 下午
 * @desc: TODO
 */
@Service
public class MyServiceB {
	@Autowired
	MyService myService;

	public MyServiceB(){

	}

	public MyServiceB(MyService myService){
		this.myService = myService;
	}
}
