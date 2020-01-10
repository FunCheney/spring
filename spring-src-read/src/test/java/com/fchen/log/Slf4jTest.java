package com.fchen.log;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Fchen
 * @date: 2020/1/10 8:16 上午
 * @desc: Slf4j 应用测试
 */
public class Slf4jTest {
	@Test
	public void testSlf4j(){
		Logger logger = LoggerFactory.getLogger("Slf4j");
		logger.info("Slf4j");
	}
}
