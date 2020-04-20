### 创建代理

```java
protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
        @Nullable Object[] specificInterceptors, TargetSource targetSource) {

    if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
        AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
    }

    ProxyFactory proxyFactory = new ProxyFactory();
    // 获取当前类中相关的属性
    proxyFactory.copyFrom(this);

    /*
     决定对于给定的 bean 是否应该使用 targetClass 而不是他的接口代理，
     检查 proxyTargetClass 设置以及 preserveTargetClass 属性
     */
    if (!proxyFactory.isProxyTargetClass()) {
        if (shouldProxyTargetClass(beanClass, beanName)) {
            proxyFactory.setProxyTargetClass(true);
        }
        else {
            evaluateProxyInterfaces(beanClass, proxyFactory);
        }
    }

    Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
    // 加强增强器
    proxyFactory.addAdvisors(advisors);
    proxyFactory.setTargetSource(targetSource);
    customizeProxyFactory(proxyFactory);

    /*
    用来控制代理工厂比配置后，是否允许修改通知。
    缺省值为 false （即在代理配置之后，不允许修改代理的配置）
     */
    proxyFactory.setFrozen(this.freezeProxy);
    if (advisorsPreFiltered()) {
        proxyFactory.setPreFiltered(true);
    }

    return proxyFactory.getProxy(getProxyClassLoader());
}
```
&ensp;&ensp;对于代理类的创建及处理，Spring 委托给了 ProxyFactory 去处理，而在此函数中主要是对
ProxyFactory 的初始化操作，进而对真正的创建代理做准备，这些初始化操作包括如下内容。


1. 获取当前类中的属性。
2. 添加代理接口。
3. 封装 Advisor 并加入到 ProxyFactory 中。
4. 这只代理的类。
5. 在Spring 中还为子类提供了定制的函数 customizeProxyFactory，子类可以在此函数中进行对 ProxyFactory的进一步封装。
6. 进行获取代理操作。


```java
protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
    // Handle prototypes correctly...
    // 解析注册所有的 InterceptorName 
    Advisor[] commonInterceptors = resolveInterceptorNames();

    List<Object> allInterceptors = new ArrayList<>();
    if (specificInterceptors != null) {
        // 加入拦截器
        allInterceptors.addAll(Arrays.asList(specificInterceptors));
        if (commonInterceptors.length > 0) {
            if (this.applyCommonInterceptorsFirst) {
                allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
            }
            else {
                allInterceptors.addAll(Arrays.asList(commonInterceptors));
            }
        }
    }
    if (logger.isTraceEnabled()) {
        int nrOfCommonInterceptors = commonInterceptors.length;
        int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
        logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
                " common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
    }

    Advisor[] advisors = new Advisor[allInterceptors.size()];
    for (int i = 0; i < allInterceptors.size(); i++) {
        // 拦截器进行封装转化为 Advisor
        advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
    }
    return advisors;
}
```

```java
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
```
#### 创建代理

```java
public Object getProxy(@Nullable ClassLoader classLoader) {
    return createAopProxy().getProxy(classLoader);
}
```

```java
protected final synchronized AopProxy createAopProxy() {
    if (!this.active) {
        activate();
    }
    /**
     * 通过 AopProxyFactory 取得 AopProxy，这个AopProxyFactory 是在初始化
     * 函数中定义的，使用的是 DefaultAopProxyFactory
     */
    return getAopProxyFactory().createAopProxy(this);
}
```

```java
public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
    if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
        Class<?> targetClass = config.getTargetClass();
        if (targetClass == null) {
            throw new AopConfigException("TargetSource cannot determine target class: " +
                    "Either an interface or a target is required for proxy creation.");
        }
        // 如果targetClass 是接口类，使用 JDK 来生成 Proxy
        if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
            return new JdkDynamicAopProxy(config);
        }
        // 不是接口生成Proxy，那么使用 CGLIB
        return new ObjenesisCglibAopProxy(config);
    }
    else {
        return new JdkDynamicAopProxy(config);
    }
}
```

&ensp;&ensp;从上述代码中，可以看到Spring创建代理的判断过程如下：

* optimize: 用来控制通过 CGLIB 创建代理的过程是否使用了激进的优化策略，除非完全了解Aop的代理如何优化，否则不推荐用户使用这个设置，目前这个属性仅用于 CGLIB代理，对于 JDK 代理无效
* proxyTargetClass：这个属性为true 时，目标类本身被代理而不是目标类的接口。如果这个类设置为true，则 CGLIB 代理被创建。
* hasNoUserSuppliedProxyInterfaces：是否存在代理接口。


&ensp;&ensp;Spring 对于使用 JDK 代理与 CGLIB 代理方式的总结：

* 如果目标对象实现了接口，默认情况下会采用 JDK 的动态代理实现 Aop。
* 如果目标对象实现了接口，可以强制使用 CGLIB 实现 Aop。
* 如果目标对象没有实现接口，必须采用 CGLIB 库，Spring 会自动在 JDK 动态代理和 CGLIB 代理之间切换。

&ensp;&ensp;如何强制使用 CGLIB 实现 Aop？

1. 添加 CGLIB 库，Spring_HOME/cglib/*.jar.
2. proxyTargetClass 属性这只为true。

&ensp;&ensp;JDK 动态代理和 CGLIB 字节码生成的区别？

* JDK 动态代理只能对实现接口的类生成代理，而不能针对类。
* CGLIB 是针对类实现代理，主要是对针对指定的类生成一个子类，覆盖其中的方法，因为是继承，所以该类的或方法最好不要声明成 final。





