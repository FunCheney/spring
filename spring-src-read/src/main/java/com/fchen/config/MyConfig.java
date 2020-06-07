package com.fchen.config;

import com.fchen.bean.biz.FactoryBeanTest;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

/**
 * @author: Fchen
 * @date: 2020/1/5 9:22 上午
 * @desc: 配置类
 */
@Configuration
@ComponentScan("com.fchen")
//@EnableAspectJAutoProxy
//@Import(MyScanConfig.class)
public class MyConfig {

	@Bean("factoryBeanTest")
	public FactoryBeanTest userFactoryBean() {
		return new FactoryBeanTest();
	}

//	@Component
//	public static class TestMy{
//
//	}

//	@Configuration
//	public static class TestMy2{
//		@Bean
//		public FactoryBeanTest userFactoryBean() {
//			return new FactoryBeanTest();
//		}
//	}
}
