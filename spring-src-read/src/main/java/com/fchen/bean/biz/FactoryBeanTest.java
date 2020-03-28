package com.fchen.bean.biz;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author: Fchen
 * @date: 2020/3/28 3:50 下午
 * @desc: FactoryBean
 */
@Component
public class FactoryBeanTest implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		return new User();
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
