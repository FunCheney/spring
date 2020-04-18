package com.fchen.config;

import com.fchen.bean.biz.FactoryBeanTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author: Fchen
 * @date: 2020/1/5 9:22 上午
 * @desc: 配置类
 */
@Configuration
@ComponentScan("com.fchen")
//@EnableAspectJAutoProxy
public class MyConfig {

	@Bean("factoryBeanTest")
	public FactoryBeanTest userFactoryBean() {
		return new FactoryBeanTest();
	}


}
