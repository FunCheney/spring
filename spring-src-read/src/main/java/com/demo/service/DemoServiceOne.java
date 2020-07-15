package com.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/7/8 8:34 下午
 * @desc: TODO
 */
@Service
public class DemoServiceOne {

	DemoServiceTwo demoServiceTwo;

	DemoServiceThree demoServiceThree;

	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	}

//	public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//		this.demoServiceThree = demoServiceThree;
//	}
	public DemoServiceOne(){

	}

//	@Autowired(required = false)
//	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//	}
//	@Autowired(required = false)
//	public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//		this.demoServiceThree = demoServiceThree;
//	}
}
