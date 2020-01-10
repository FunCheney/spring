package com.fchen.log;

import org.junit.Test;

import java.util.logging.Logger;

/**
 * @author: Fchen
 * @date: 2020/1/9 11:35 下午
 * @desc: TODO
 */
public class JulTest {
	@Test
	public void testJul(){
		Logger logger = Logger.getLogger("jul");
		logger.info("jul");
	}
}
