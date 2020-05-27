## AnnotationConfigApplicationContext 源码解析
```
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
    /**
     * 调用默认的构造方法，由于该类有父类，
     * 故而先调用父类的构造方法，在调用自己的构造方法
     * 在自己的构造方法中初始一个读取器和一个扫描器，
     * 第①部分！！！
     */
    this();
    /**
     * 向spring 的容器中注册bean
     * 第②部分！！！
     */
    register(componentClasses);
    /**
     * 初始化spring的环境
     * 第③部分！！！
     */
    refresh();
}
```
&ensp;&ensp;这里将要解析上述代码中的`refresh()`这一部分的代码。。。
### refresh()方法实现
```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // Prepare this context for refreshing.
        /**
         * 准备工作包括设置启动时间，是否激活标识位
         * 初始化属性源（property source）配置
         */
        prepareRefresh();

        // Tell the subclass to refresh the internal bean factory.
        /**
         * 获取 DefaultListableBeanFactory 对象，后续会对BeanFactory设置
         * 在子类中启动refreshBeanFactory()的地方
         */
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // Prepare the bean factory for use in this context.
        /**
         * 准备BeanFactory
         */
        prepareBeanFactory(beanFactory);

        try {
            // Allows post-processing of the bean factory in context subclasses.
            /** 当前未写任何代码，是一个空方法*/
            postProcessBeanFactory(beanFactory);

            // Invoke factory processors registered as beans in the context.
            /**
             * 在spring的环境中 执行已经被注册的 BeanFactoryPostProcessors
             * 设置执行自定义的 ProcessorBeanFactory
             * 这里是重点代码
             */
            invokeBeanFactoryPostProcessors(beanFactory);

            // Register bean processors that intercept bean creation.
            /** 注册spring Bean 的后置处理器*/
            registerBeanPostProcessors(beanFactory);

            // Initialize message source for this context.
            /** 对上下文中的消息源进行初始化*/
            initMessageSource();

            // Initialize event multicaster for this context.
            /** 初始化上下文中的事件机制*/
            initApplicationEventMulticaster();

            // Initialize other special beans in specific context subclasses.
            /** 没有具体实现的方法*/
            onRefresh();

            // Check for listener beans and register them.
            /** 检查监听bean并且将这些Bean向容器注册*/
            registerListeners();

            // Instantiate all remaining (non-lazy-init) singletons.
            /** Bean的实例化*/
            finishBeanFactoryInitialization(beanFactory);

            // Last step: publish corresponding event.
            /** 发布容器事件，结束refresh过程*/
            finishRefresh();
        }

        catch (BeansException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Exception encountered during context initialization - " +
                        "cancelling refresh attempt: " + ex);
            }

            // Destroy already created singletons to avoid dangling resources.
            /** 为防止Bean资源占用，在异常处理中，销毁已经在前面过程中生成的单件bean*/
            destroyBeans();

            // Reset 'active' flag.
            /** 重置 active 标志*/
            cancelRefresh(ex);

            // Propagate exception to caller.
            throw ex;
        }

        finally {
            // Reset common introspection caches in Spring's core, since we
            // might not ever need metadata for singleton beans anymore...
            resetCommonCaches();
        }
    }
}
```
### refresh()时序图
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotationConfigApplication_refresh.jpg">
 </div>

&ensp;&ensp;上述的流程图中，在AbstractApplicationContext#refresh()发放中有10多个流程，一起看一下每个流程的细节。
这个refresh()方法是IoC容器的核心方法，无论是基于**Xml**形式的，还是基于**注解**形式的方法都会调用这个方法，来完成容器的初始化。

#### 1.prepareRefresh()方法
&ensp;&ensp;该方法包括主要是完成：准备工作包括设置启动时间，是否激活标识位；初始化属性源（property source）配置。

```java
protected void prepareRefresh() {
    // Switch to active.
    this.startupDate = System.currentTimeMillis();
    this.closed.set(false);
    this.active.set(true);

    if (logger.isDebugEnabled()) {
        if (logger.isTraceEnabled()) {
            logger.trace("Refreshing " + this);
        }
        else {
            logger.debug("Refreshing " + getDisplayName());
        }
    }

    /**
     * 留给子类覆盖
     * 符合spring的开放式结构设计，给用户扩展spring提供了可能
     * 用户可根据自身需要重写initPropertySources()方法，并在方法中进行个性化
     * 的属性处理及设置
     * 第①处！！！
     */
    initPropertySources();

    /** 验证需要的属性文件是否都已经放入环境中*/
    getEnvironment().validateRequiredProperties();

    /** 存储预先刷新的容器监听器*/
    if (this.earlyApplicationListeners == null) {
        this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
    }
    else {
        // Reset local application listeners to pre-refresh state.
        this.applicationListeners.clear();
        this.applicationListeners.addAll(this.earlyApplicationListeners);
    }

    // Allow for the collection of early ApplicationEvents,
    // to be published once the multicaster is available...
    this.earlyApplicationEvents = new LinkedHashSet<>();
}
```
&ensp;&ensp;其中第一处的代码，是一个空壳方法，spring并没有实现。留做以后子类扩展使用，具体代码如下
```java
protected void initPropertySources() {
    // For subclasses: do nothing by default.
}
```

#### 2.obtainFreshBeanFactory()方法
&ensp;&ensp;该方法是获取BeanFactory的地方，这里一般都是获取Spring 默认的实现DefaultListableBeanFactory.
这获取BeanFactory的实现时，这里采用委派设计模式，将其交给不同的子类去实现。
```
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    /** 这里使用了委派设计模式，父类定义了抽象方法，具体的实现通过子类实现 */
    refreshBeanFactory();
    /** 返回当前实体的 beanFactory属性 */
    return getBeanFactory();
}
```
&ensp;&ensp;在这里，我们采取的是基于注解的方式，最终执行的代码是`GenericApplicationContext#refreshBeanFactory()`方法。
```
protected final void refreshBeanFactory() throws IllegalStateException {
    if (!this.refreshed.compareAndSet(false, true)) {
        throw new IllegalStateException(
                "GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
    }
    this.beanFactory.setSerializationId(getId());
}
```
&ensp;&ensp;同理这里的`getBeanFactory()`方法，也会到`GenericApplicationContext`中执行`getBeanFactory()`方法，
最终获取到的是在之前初始化好的DefaultListableBeanFactory对象。
```java
public final ConfigurableListableBeanFactory getBeanFactory() {
    return this.beanFactory;
}
```

#### 3.prepareBeanFactory(beanFactory)方法
&ensp;&ensp;该方法是准备BeanFactory的方法，配置BeanFactory的标准特征，比如上下文的加载器 ClassLoader 和 post-processors 回调等。
```java
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    // Tell the internal bean factory to use the context's class loader etc.
    /** 设置bean的类加载器*/
    beanFactory.setBeanClassLoader(getClassLoader());
    /** 设置bean表达式解释器*/
    beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));

    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

    // Configure the bean factory with context callbacks.
    /**
     * 工厂中添加一个bean的后置处理器
     * 目的就是 注册一个 BeanPostProcessor，真正的逻辑是在 ApplicationContextAwareProcessor
     * 在bean实例化的时候，spring 激活 bean的 init-method 前后 会调用 BeanPostProcessor 的
     * postProcessAfterInitialization() 和 postProcessBeforeInitialization()
     * 因此，这里 要看一下 ApplicationContextAwareProcessor 类中的这两个方法
     */
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    /**
     * 自动注入被忽略的类
     * spring 将 ApplicationContextAwareProcessor 注册后，在invokeAwareInterfaces()
     * 方法中间接调用的 Aware 类已经不再是 普通的 Bean 了，如：
     * ResourceLoaderAware, ApplicationEventPublisherAware, ApplicationContextAware 等
     * 那么就要在spring 做依赖注入的时候忽略掉它们，ignoreDependencyInterface() 就是这个作用。
     */
    beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
    beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
    beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
    beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

    // BeanFactory interface not registered as resolvable type in a plain factory.
    // MessageSource registered (and found for autowiring) as a bean.
    /**
     * 设置几个自动装配的特殊规则
     * 注册依赖注入的功能，如，当对注册了对 BeanFactory 的依赖解析后，
     * 当 bean 的属性注入的时候，一旦检测到 属性为 BeanFactory 类型
     * 便将 beanFactory 的实例注入进去
     */
    beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
    beanFactory.registerResolvableDependency(ResourceLoader.class, this);
    beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
    beanFactory.registerResolvableDependency(ApplicationContext.class, this);

    // Register early post-processor for detecting inner beans as ApplicationListeners.
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

    // Detect a LoadTimeWeaver and prepare for weaving, if found.
    /** 增加对AspectJ的支持*/
    if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
        beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
        // Set a temporary ClassLoader for type matching.
        beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }

    // Register default environment beans.
    /** 添加默认的系统环境bean*/
    if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
        beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
    }
}
```
#### 4.postProcessBeanFactory()
&ensp;&ensp; `postProcessBeanFactory()` 同样也是一个空方法，交给子类实现。
```java
protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
}
```
### 容器形成图
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/annotionConfigApplication/spring_ioc_contains_3.jpg">
 </div>