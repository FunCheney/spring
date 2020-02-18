package com.fchen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Fchen
 * @date: 2020/2/18 10:53 下午
 * @desc: ClassPathAppContextService
 */
public class ClassPathAppContextService {
	private Logger log = LoggerFactory.getLogger(ClassPathAppContextService.class);

	public void test(){
		System.out.println("ClassPathAppContextService test ...");
		log.info("ClassPathAppContextService test ...");
	}
}
