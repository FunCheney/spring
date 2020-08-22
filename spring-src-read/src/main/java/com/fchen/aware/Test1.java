package com.fchen.aware;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/8/22 5:34 下午
 * @desc: TODO
 */
@Service
@Scope("prototype")
public abstract class Test1 {
	@Lookup
	protected abstract Test1 methodInject();

	public void test(){
		Test1 test1 = methodInject();
		System.out.println("Test ->" + test1);
	}
}
