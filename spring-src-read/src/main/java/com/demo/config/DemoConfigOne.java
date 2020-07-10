package com.demo.config;

import com.demo.factory.FactoryBeanDemoOne;
import com.demo.service.DemoServiceOne;
import com.demo.service.DemoServiceTwo;
import com.fchen.bean.biz.FactoryBeanTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author: Fchen
 * @date: 2020/7/7 11:54 下午
 * @desc: TODO
 */
@Configuration
@ComponentScan("com.demo")
public class DemoConfigOne {

	@Bean
	public FactoryBeanDemoOne demoFactoryBean() {
		return new FactoryBeanDemoOne();
	}

	@Bean
	public  DemoServiceOne demoServiceOne(DemoServiceTwo demoServiceTwo){
		return new DemoServiceOne(demoServiceTwo);
	}

	@Bean
	public  DemoServiceOne demoServiceOne(){
		return new DemoServiceOne();
	}
}
