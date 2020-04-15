### 创建Aop代理
&ensp;&ensp;AnnotationAwareAspectJAutoProxyCreator 实现了 BeanPostProcessor 接口，
当 Spring 加载壮这个 Bean 时会在实例化之前调用 postProcessAfterInitialization 这个是 Aop 开始的地方。

`AbstractAutoProxyCreator#postProcessAfterInitialization()`

```java
public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
    if (bean != null) {
        // 根据给定的 bean 的 class 和 name 构建出 key。格式: beanClassName_beanName
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        if (this.earlyProxyReferences.remove(cacheKey) != bean) {
            // 如果他适合被代理，需要封装指定的 bean
            return wrapIfNecessary(bean, beanName, cacheKey);
        }
    }
    return bean;
}
```

```
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    // 如果已被处理
    if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
        return bean;
    }
    // 无需增强
    if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
        return bean;
    }
    // 给定的 bean 类是否代表一个基础设施类，基础设施类不应代理， 或者配置了指定的 bean 不需要自动代理
    if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }

    // Create proxy if we have advice.
    // 如果存在增强方法则创建代理
    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
    // 如果获取到了增强则需要针对增强创建代理
    if (specificInterceptors != DO_NOT_PROXY) {
        this.advisedBeans.put(cacheKey, Boolean.TRUE);
        // 创建代理
        Object proxy = createProxy(
                bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
        this.proxyTypes.put(cacheKey, proxy.getClass());
        return proxy;
    }

    this.advisedBeans.put(cacheKey, Boolean.FALSE);
    return bean;
}
```
时序图如下：


&ensp;&ensp;创建代理逻辑主要包含两个步骤：

- 1. 获取增强方法或增强器
- 2. 根据获取的增强进行代理

&ensp;&ensp;首先来看一下获取增强方法的实现逻辑：

```java
protected Object[] getAdvicesAndAdvisorsForBean(
        Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

    List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
    if (advisors.isEmpty()) {
        return DO_NOT_PROXY;
    }
    return advisors.toArray();
}
```

```java
protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    extendAdvisors(eligibleAdvisors);
    if (!eligibleAdvisors.isEmpty()) {
        eligibleAdvisors = sortAdvisors(eligibleAdvisors);
    }
    return eligibleAdvisors;
}
```
&ensp;&ensp;对于指定 bean 的增强方法的获取 包含两个步骤，获取所有的增强以及寻找所有增强中适用于 bean 的增强并应用，
`findCandidateAdvisors()` 与 `findAdvisorsThatCanApply()`变是做这两件事的。


