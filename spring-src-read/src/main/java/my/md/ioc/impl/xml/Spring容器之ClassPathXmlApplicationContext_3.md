## ClassPathXmlApplicationContext 的源码阅读

### `AbstractAutowireCapableBeanFactory#createBean()` 方法

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException {

		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// Make sure bean class is actually resolved at this point, and
		// clone the bean definition in case of a dynamically resolved Class
		// which cannot be stored in the shared merged bean definition.
		// 判断需要创建的Bean是否可以实例化，这个类是否可以通过类装载器载入
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    // Prepare method overrides.
    try {
        //处理lockup-method 和 replace-method配置，spring将这两个方法统称为 method overrides
        mbdToUse.prepareMethodOverrides();
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                beanName, "Validation of method overrides failed", ex);
    }

    try {
        // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
        // 如果Bean配置了PostProcessor，这里返回的是一个Proxy
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
        if (bean != null) {
            // 在配置了PostProcessor的处理器中 改变了Bean，直接返回
            return bean;
        }
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                "BeanPostProcessor before instantiation of bean failed", ex);
    }

    try {
        /**
         * 创建Bean的实例，若bean的配置信息中配置了 lookup-method 和 replace-method
         */
        Object beanInstance = doCreateBean(beanName, mbdToUse, args);
        if (logger.isTraceEnabled()) {
            logger.trace("Finished creating instance of bean '" + beanName + "'");
        }
        return beanInstance;
    }
    catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
        // A previously detected exception with proper bean creation context already,
        // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
    }
}
```
#### AbstractBeanDefinition#prepareMethodOverrides()
```java
public void prepareMethodOverrides() throws BeanDefinitionValidationException {
    // Check that lookup methods exist and determine their overloaded status.
    if (hasMethodOverrides()) {
        getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
    }
}

protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
    /** 获取对应类中对应方法名的个数*/
    int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
    if (count == 0) {
        throw new BeanDefinitionValidationException(
                "Invalid method override: no method with name '" + mo.getMethodName() +
                "' on class [" + getBeanClassName() + "]");
    }
    else if (count == 1) {
        // Mark override as not overloaded, to avoid the overhead of arg type checking.
        /** 标记MethodOverride暂未被覆盖，避免参数类型检查的开销*/
        mo.setOverloaded(false);
    }
}
```

#### ClassUtils#getMethodCountForName()
```java
public static int getMethodCountForName(Class<?> clazz, String methodName) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(methodName, "Method name must not be null");
    int count = 0;
    Method[] declaredMethods = clazz.getDeclaredMethods();
    for (Method method : declaredMethods) {
        if (methodName.equals(method.getName())) {
            count++;
        }
    }
    Class<?>[] ifcs = clazz.getInterfaces();
    for (Class<?> ifc : ifcs) {
        count += getMethodCountForName(ifc, methodName);
    }
    if (clazz.getSuperclass() != null) {
        count += getMethodCountForName(clazz.getSuperclass(), methodName);
    }
    return count;
}
```

#### AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation()
```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
    Object bean = null;
    /** 如果尚未被解析*/
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
        // Make sure bean class is actually resolved at this point.
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            Class<?> targetType = determineTargetType(beanName, mbd);
            if (targetType != null) {
                // bean的前置处理器
                bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                if (bean != null) {
                    // bean的后置处理器
                    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                }
            }
        }
        mbd.beforeInstantiationResolved = (bean != null);
    }
    return bean;
}
```