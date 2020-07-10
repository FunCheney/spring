package com.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/7/8 8:34 下午
 * @desc: TODO
 */

public class DemoServiceOne {
	@Autowired
	DemoServiceTwo demoServiceTwo;
	public DemoServiceOne(){

	}

	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	}
}
