## invokeBeanFactoryPostProcessors
断点调试图一：

```java
private static void invokeBeanFactoryPostProcessors(
        Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

    for (BeanFactoryPostProcessor postProcessor : postProcessors) {
        /**
         * 根据不同的 BeanFactoryPostProcessor 实现
         *  去调用不同的 postProcessBeanFactory 方法
         *  ConfigurationClassPostProcessor 同时也是 BeanFactoryPostProcessor 的子类
         */
        postProcessor.postProcessBeanFactory(beanFactory);
    }
}
```
&ensp;&ensp;从上图中可以看出，这里的 `postProcessor` 对应的子类为 `ConfigurationClassPostProcessor`，下面的 `postProcessBeanFactory()`
方法对应子类的实现如下：
```java
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    int factoryId = System.identityHashCode(beanFactory);
    if (this.factoriesPostProcessed.contains(factoryId)) {
        throw new IllegalStateException(
                "postProcessBeanFactory already called on this post-processor against " + beanFactory);
    }
    this.factoriesPostProcessed.add(factoryId);
    if (!this.registriesPostProcessed.contains(factoryId)) {
        // BeanDefinitionRegistryPostProcessor hook apparently not supported...
        // Simply call processConfigurationClasses lazily at this point then.
        processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
    }
    /**
     * 配置类 产生 cglib 代理
     */
    enhanceConfigurationClasses(beanFactory);
    beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
}
```

```java
public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
    Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
        BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
        /**
         * 判断这个类是不是全注解类,这个地方与前面 {@link Configuration} 注解的类的
         * 处理有关
         */
        if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
            if (!(beanDef instanceof AbstractBeanDefinition)) {
                throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
                        beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
            }
            else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
                logger.info("Cannot enhance @Configuration bean definition '" + beanName +
                        "' since its singleton instance has been created too early. The typical cause " +
                        "is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
                        "return type: Consider declaring such methods as 'static'.");
            }
            /** 如果是全注解类，就将其 put 到 configBeanDefs 中*/
            configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
        }
    }
    if (configBeanDefs.isEmpty()) {
        /**
         * Map 为空 表示没有全注解类，则返回
         */
        // nothing to enhance -> return immediately
        return;
    }

    ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
    for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
        AbstractBeanDefinition beanDef = entry.getValue();
        // If a @Configuration class gets proxied, always proxy the target class
        beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
        try {
            // Set enhanced subclass of the user-specified bean class
            /**
             * 对全注解类 进行 cglib 代理
             * config 类 -> cglib class -> BeanDefinition -> bean
             */
            Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
            if (configClass != null) {
                Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
                if (configClass != enhancedClass) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
                                "enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
                    }
                    beanDef.setBeanClass(enhancedClass);
                }
            }
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
        }
    }
}
```
&ensp;&ensp;接上篇文章我们提到配置类的 `Full`和 `Lite`两种不同的模式。在这篇文章中，我们可以看到，`Full`模式，也就是全配置类，Spring 通过使用`CGLIB`动态代理的
方式对其进行了增强。而 `Lite` 模式的配置类，没有通过代理的方式增强。我们究其原因发现，对于`@Configuration` 类的处理，是Spring 的后置处理器的典型应用。纵观整个 Spring，
在器内部只有一个 `ConfigurationClassPostProcessor` 该类中处理了 `BeanDefinitionRegistryPostProcessor
` 的方法 `postProcessBeanDefinitionRegistry()`
也处理了 `postProcessBeanFactory`的方法 `postProcessBeanFactory()`。通过该类，我们应该也要知道，对于Spring 的扩展点 `BeanFactoryPostProcessor` 的处理。

```java
public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
    /** 判断是否被代理过*/
    if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Ignoring request to enhance %s as it has " +
                    "already been enhanced. This usually indicates that more than one " +
                    "ConfigurationClassPostProcessor has been registered (e.g. via " +
                    "<context:annotation-config>). This is harmless, but you may " +
                    "want check your configuration and remove one CCPP if possible",
                    configClass.getName()));
        }
        return configClass;
    }
    /** 没有被代理 cglib 代理*/
    Class<?> enhancedClass = createClass(newEnhancer(configClass, classLoader));
    if (logger.isTraceEnabled()) {
        logger.trace(String.format("Successfully enhanced %s; enhanced class name is: %s",
                configClass.getName(), enhancedClass.getName()));
    }
    return enhancedClass;
}
```
```java
/**
 * Creates a new CGLIB {@link Enhancer} instance.
 * 创建一个 CGLIB 实例
 */
private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
    Enhancer enhancer = new Enhancer();
    /** 增强父类 */
    enhancer.setSuperclass(configSuperClass);
    /** 增强接口，
     * 便于判断，表示一个类被增强了
     * EnhancedConfiguration 实现了 BeanFactoryAware 接口
     */
    enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
    enhancer.setUseFactory(false);
    /**
     * BeanFactoryAwareGeneratorStrategy 是一个生成策略
     * 主要为生成的 cglib 类中添加成员变量 $$beanFactory
     * 同时基于接口 EnhancedConfiguration 的父接口 BeanFactoryAware 中的 setBeanFactory 方法，
     * 设置此变量的值为当前 context 中的 beanFactory，这样一来 cglib 代理的对象就有了 beanFactory
     * 有了 factory 就能获得对象了，不用通过 new 来获取对象了
     * 该BeanFactory 的作用是在 this 调用时拦截该调用，并直接在 beanFactory 中获得目标bean
     *
     */
    enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
    enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
    enhancer.setCallbackFilter(CALLBACK_FILTER);
    enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
    return enhancer;
}
```




