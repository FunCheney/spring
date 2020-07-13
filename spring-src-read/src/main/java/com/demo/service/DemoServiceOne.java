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

	@Autowired
	DemoServiceThree demoServiceThree;
	@Autowired
	DemoServiceTwo demoServiceTwo;

//	public DemoServiceOne(){
//
//	}

//	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//	}
//
//	public DemoServiceOne(DemoServiceOne demoServiceOne, DemoServiceTwo demoServiceTwo){
//		this.demoServiceTwo = demoServiceTwo;
//		this.demoServiceOne = demoServiceOne;
//	}
}
