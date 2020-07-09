package com.demo.config;

import com.demo.factory.FactoryBeanDemoOne;
import com.demo.service.DemoServiceOne;
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
	public FactoryBeanDemoOne DemoFactoryBean() {
		return new FactoryBeanDemoOne();
	}

	@Bean
	public  FactoryBeanDemoOne DemoFactoryBean(int i){
		return new FactoryBeanDemoOne(i);
	}

	@Bean("demoService")
	public static DemoServiceOne demoServiceOne(){
		return new DemoServiceOne();
	}
}
