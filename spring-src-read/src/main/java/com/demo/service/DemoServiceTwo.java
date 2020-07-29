package com.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author: Fchen
 * @date: 2020/7/10 7:58 上午
 * @desc: TODO
 */
@Service
public class DemoServiceTwo {

//	@Autowired
//	DemoServiceThree demoServiceThree;

//	@Autowired
//	public DemoServiceTwo(DemoServiceOne demoServiceOne){
//		this.demoServiceOne = demoServiceOne;
//	}
	@Autowired
	DemoServiceOne demoServiceOne;
}
