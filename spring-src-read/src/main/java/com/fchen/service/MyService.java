package com.fchen.service;

import com.fchen.dao.MyTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Fchen
 * @date: 2020/1/5 9:23 上午
 * @desc: spring 测试类
 */
@Service
public class MyService {

	private Logger log = LoggerFactory.getLogger(MyService.class);

	//	MyTest xxxx;
//	@Autowired
//	public void setxxx(MyTest xxxx) {
//		this.xxxx = xxxx;
//	}
	@Autowired
	MyTest myTest;
	@Autowired
	MyServiceB myServiceB;

	public void test(){
		System.out.println("hello test");
		myTest.test();
	}

//	public MyService(){
//
//	}
//
//	public MyService(MyTest myTest){
//		this.myTest = myTest;
//	}
//
//	public MyService(int t){
//
//	}
}
