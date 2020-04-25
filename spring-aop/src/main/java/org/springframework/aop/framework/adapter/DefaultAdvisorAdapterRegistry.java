/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * Default implementation of the {@link AdvisorAdapterRegistry} interface.
 * Supports {@link org.aopalliance.intercept.MethodInterceptor},
 * {@link org.springframework.aop.MethodBeforeAdvice},
 * {@link org.springframework.aop.AfterReturningAdvice},
 * {@link org.springframework.aop.ThrowsAdvice}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

	/**
	 * 持有一个 AdvisorAdapter 的 list，这个list 中的 Adapter 实现
	 * 与实现 Spring Aop 的 advice 增强功能想对应
	 */
	private final List<AdvisorAdapter> adapters = new ArrayList<>(3);


	/**
	 * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
	 * 这里把 已有的 advice 实现的 Adapter 加入进来，有非常熟悉的 MethodBeforeAdviceAdapter,
	 * AfterReturningAdviceAdapter, ThrowsAdviceAdapter
	 * 这些 AOP 的封装与实现。
	 */
	public DefaultAdvisorAdapterRegistry() {
		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}


	@Override
	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
		// 如果封装的对象本身就是 Advisor 类型的那么无需做过多的处理
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
		// 因为此封装方法只对 Advisor 与 Advice 两种数据有效，如果不是将不能封装
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}

		Advice advice = (Advice) adviceObject;
		if (advice instanceof MethodInterceptor) {
			// 如果是 MethodInterceptor 类型则使用 DefaultPointcutAdvisor 封装
			return new DefaultPointcutAdvisor(advice);
		}
		// 如果存在 AdvisorAdapter （Advisor 的适配器）也同样需要封装
		for (AdvisorAdapter adapter : this.adapters) {
			// Check that it is supported.
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	/**
	 * 这里是在 DefaultAdvisorChainFactory 中启动的 getInterceptors 方法
	 * @param advisor the Advisor to find an interceptor for
	 * @return
	 * @throws UnknownAdviceTypeException
	 */
	@Override
	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<>(3);
		// 从 Advisor 通知器配置中取得 advice 通知
		Advice advice = advisor.getAdvice();
		if (advice instanceof MethodInterceptor) {
			// 如果通知是 MethodInterceptor 类型的通知，直接加入 interceptors 的 list 中，不需要适配
			interceptors.add((MethodInterceptor) advice);
		}
		/**
		 * 对通知进行适配，使用已经配置好的 Adapter：MethodBeforeAdviceAdapter, AfterReturningAdviceAdapter,
		 * ThrowsAdviceAdapter，然后从对应的 adapter 中取出封装好的 AOP 编织功能的拦截器
		 */
		for (AdvisorAdapter adapter : this.adapters) {
			if (adapter.supportsAdvice(advice)) {
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
		return interceptors.toArray(new MethodInterceptor[0]);
	}

	@Override
	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		this.adapters.add(adapter);
	}

}
