### 初始化对象
&ensp;&ensp;之前的文章中我们学习了容器中对象的实例化，对象间依赖关系的处理。今天这篇文章就来看看对象的
初始化。

```java
protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            invokeAwareMethods(beanName, bean);
            return null;
        }, getAccessControlContext());
    }
    else {
        /**
         * 对特殊的bean处理：Aware，BeanClassLoaderAware，BeanFactoryAware
         */
        invokeAwareMethods(beanName, bean);
    }

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        /** 执行BeanPostProcessors的before 方法*/
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        /**
         * 激活用户自定义的 init 方法
         */
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                (mbd != null ? mbd.getResourceDescription() : null),
                beanName, "Invocation of init method failed", ex);
    }
    if (mbd == null || !mbd.isSynthetic()) {
        /** 执行BeanPostProcessors的after 方法*/
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    return wrappedBean;
}
```

```java
private void invokeAwareMethods(final String beanName, final Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        if (bean instanceof BeanClassLoaderAware) {
            ClassLoader bcl = getBeanClassLoader();
            if (bcl != null) {
                ((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
            }
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
        }
    }
}
```

BeanPostProcessor的postProcessBeforeInitialization方法



```java
protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd)
        throws Throwable {

    /** 首先检查是否是 InitializingBean，如果是的话需要调用afterPropertiesSet方法*/
    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        if (logger.isTraceEnabled()) {
            logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
        }
        if (System.getSecurityManager() != null) {
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    ((InitializingBean) bean).afterPropertiesSet();
                    return null;
                }, getAccessControlContext());
            }
            catch (PrivilegedActionException pae) {
                throw pae.getException();
            }
        }
        else {
            // 属性 初始化后的处理，调用 afterPropertiesSet()方法
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }
    /**
     * 从代码看出：
     *  afterPropertiesSet() 和 init-method() 都是在初始化bean时执行，
     *  执行顺序是 先 afterPropertiesSet()方法执行，
     *           然后 init-method() 方法执行
     */
    if (mbd != null && bean.getClass() != NullBean.class) {
        String initMethodName = mbd.getInitMethodName();
        if (StringUtils.hasLength(initMethodName) &&
                !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
                !mbd.isExternallyManagedInitMethod(initMethodName)) {
            // 调用自定义的初始化方法
            invokeCustomInitMethod(beanName, bean, mbd);
        }
    }
}
```

```java
protected void invokeCustomInitMethod(String beanName, final Object bean, RootBeanDefinition mbd)
        throws Throwable {

    /**
     * 获取 Bean 定义的 @Bean 中的 initMethod 属性 或xml中init-Method
     */
    String initMethodName = mbd.getInitMethodName();
    Assert.state(initMethodName != null, "No init method set");
    Method initMethod = (mbd.isNonPublicAccessAllowed() ?
            BeanUtils.findMethod(bean.getClass(), initMethodName) :
            ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName));

    if (initMethod == null) {
        if (mbd.isEnforceInitMethod()) {
            throw new BeanDefinitionValidationException("Could not find an init method named '" +
                    initMethodName + "' on bean with name '" + beanName + "'");
        }
        else {
            if (logger.isTraceEnabled()) {
                logger.trace("No default init method named '" + initMethodName +
                        "' found on bean with name '" + beanName + "'");
            }
            // Ignore non-existent default lifecycle methods.
            return;
        }
    }

    if (logger.isTraceEnabled()) {
        logger.trace("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
    }
    Method methodToInvoke = ClassUtils.getInterfaceMethodIfPossible(initMethod);

    if (System.getSecurityManager() != null) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            ReflectionUtils.makeAccessible(methodToInvoke);
            return null;
        });
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
                    methodToInvoke.invoke(bean), getAccessControlContext());
        }
        catch (PrivilegedActionException pae) {
            InvocationTargetException ex = (InvocationTargetException) pae.getException();
            throw ex.getTargetException();
        }
    }
    else {
        try {
            ReflectionUtils.makeAccessible(methodToInvoke);
            /**
             * 通过 JDk 的反射机制得到 Method 对象
             * 直接调用在 Bean 中定义声明的初始化方法
             */
            methodToInvoke.invoke(bean);
        }
        catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
```
&ensp;&ensp;