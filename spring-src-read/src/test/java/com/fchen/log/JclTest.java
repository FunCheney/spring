package com.fchen.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * @author: Fchen
 * @date: 2020/1/9 11:50 下午
 * @desc: 测试jcl应用类
 */
public class JclTest {
	@Test
	public void testJcl(){
      Log log = LogFactory.getLog(JclTest.class);
      log.info("Jcl");
	}
}
