package com.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Fchen
 * @date: 2020/7/8 8:34 下午
 * @desc: TODO
 */
@Service
public class DemoServiceOne {

//	@Autowired
//	List<IStrategryService> myList;
//	@Autowired
	DemoServiceTwo demoServiceTwo;

//	public void test(){
//		System.out.println("hello");
//	}

//	DemoServiceThree demoServiceThree;

	@Autowired
	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	}

//	public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//		this.demoServiceThree = demoServiceThree;
//	}
//	@Autowired(required = false)
//	public DemoServiceOne(){
//
//	}

//	@Autowired
//	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//	}
//	@Autowired(required = false)
//	public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//		this.demoServiceThree = demoServiceThree;
//	}

//	@Autowired(required = false)
//	public DemoServiceOne(DemoServiceThree demoServiceThree){
//		this.demoServiceThree = demoServiceThree;
//	}
}
