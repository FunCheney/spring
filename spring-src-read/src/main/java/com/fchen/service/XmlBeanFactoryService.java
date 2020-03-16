package com.fchen.service;

import com.fchen.dao.MyTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author: Fchen
 * @date: 2020/2/19 10:46 下午
 * @desc: XmlBeanFactoryService
 */
public class XmlBeanFactoryService {

	@Autowired
	MyTest myTest;

	public void say(){
		myTest.test();
		System.out.println("hello");
	}
}
