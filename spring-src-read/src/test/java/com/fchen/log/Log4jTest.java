package com.fchen.log;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * @author: Fchen
 * @date: 2020/1/9 11:33 下午
 * @desc: log4j 应用测视类
 */
public class Log4jTest {
	@Test
	public void testLog4j(){
		Logger logger = Logger.getLogger(Log4jTest.class);
		logger.info("log4j");
	}
}
