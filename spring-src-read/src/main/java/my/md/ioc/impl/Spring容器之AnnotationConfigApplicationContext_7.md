## Spring容器初始化 refresh() 方法_04

### 6.registerBeanPostProcessors

```java
protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
}
```
&ensp;&ensp;在Spring中对于 `BeanPostProcessor` 的处理分为连个过程，对于内置的 `BeanPostProcessor`的子类的实现，添加到
`IoC`容器中，是通过`refresh()` 方法中的 `prepareBeanFactory()` 过程来完成的。比如 `ApplicationContextAwareProcessor`，
`ApplicationListenerDetector`。对于 `ImportAwareBeanPostProcessor` 是在，处理 `ConfigurationClassPostProcessor`时，对
`@Configuration`注解的类生成`CGLIB` 代理的时候加入容器中的。最后，在Spring中对于 `BeanPostProcessor`的实现类的处理，是在 `refresh()`
方法中有一个步骤 `registerBeanPostProcessors()` 来完成注册的，如 `BeanPostProcessorChecker`，`CommonAnnotationBeanPostProcessor`
以及 `AutowiredAnnotationBeanPostProcessor`，`CommonAnnotationBeanPostProcessor`。这样，在容器初始化之处，没有自己实现 `BeanPsotProcessor`
的时候，容器中就包含了6个Bean的后置处理器的实现。其中对于 `registerBeanPostProcessors()` 方法的详细代码实现，下面一一分析：

```java
public static void registerBeanPostProcessors(
        ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

    // 获取BeanDefinitionMap中所有的BeanPostProcessor
    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

    /**
     * BeanPostProcessorChecker 是一个普通的信息打印，可能会有些情况
     * 当spring 的配置中的后置处理器还没有被注册就已经开始了bean的初始化时
     * 便会打印出 BeanPostProcessorChecker 中设定的值
     */
    int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
    // new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount)
    // 创建一个 默认的 PostProcessorRegistrationDelegate, BeanPostProcessorChecker 是 PostProcessorRegistrationDelegate
    // 中的一个静态的内部类 且实现了 BeanPostProcessor 接口
    beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

    /**
     * 使用priorityOrdered 保证顺序
     */
    List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
    List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
    /**
     * 使用 ordered 保证顺序
     */
    List<String> orderedPostProcessorNames = new ArrayList<>();
    /**
     * 无序 BeanPostProcessor
     */
    List<String> nonOrderedPostProcessorNames = new ArrayList<>();
    for (String ppName : postProcessorNames) {
        if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            priorityOrderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
            orderedPostProcessorNames.add(ppName);
        }
        else {
            nonOrderedPostProcessorNames.add(ppName);
        }
    }

    /**
     * 第一步，注册所有实现 PriorityOrdered 的 BeanPostProcessors
     */
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

    /**
     * 第二步，注册所有实现 Ordered 的 BeanPostProcessors
     */
    List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
    for (String ppName : orderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        orderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, orderedPostProcessors);

    /**
     * 第三步，注册无序 的 BeanPostProcessors
     */
    List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
    for (String ppName : nonOrderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        nonOrderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

    // Finally, re-register all internal BeanPostProcessors.
    /**
     * 注册所有 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessors
     */
    sortPostProcessors(internalPostProcessors, beanFactory);
    registerBeanPostProcessors(beanFactory, internalPostProcessors);

    // Re-register post-processor for detecting inner beans as ApplicationListeners,
    // moving it to the end of the processor chain (for picking up proxies etc).
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
}
```
#### 将Bean的后置处理器加入到容器中
```java
public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
    // Remove from old position, if any
    this.beanPostProcessors.remove(beanPostProcessor);
    // Track whether it is instantiation/destruction aware
    if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
        this.hasInstantiationAwareBeanPostProcessors = true;
    }
    if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
        this.hasDestructionAwareBeanPostProcessors = true;
    }
    // Add to end of list
    this.beanPostProcessors.add(beanPostProcessor);
}
```
&ensp;&ensp;至此，Spring 中容器初始化不部分的解析已经处理完了，到目前为止，容器的中的东西基本上已经齐活了，通过下面的容器形成图示例：
spring_iod_contains_5.jpg

### 7.initMessageSource()

```java
protected void initMessageSource() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
        this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
        // Make MessageSource aware of parent MessageSource.
        if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
            HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
            if (hms.getParentMessageSource() == null) {
                // Only set parent context as parent MessageSource if no parent MessageSource
                // registered already.
                hms.setParentMessageSource(getInternalParentMessageSource());
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Using MessageSource [" + this.messageSource + "]");
        }
    }
    else {
        // Use empty MessageSource to be able to accept getMessage calls.
        DelegatingMessageSource dms = new DelegatingMessageSource();
        dms.setParentMessageSource(getInternalParentMessageSource());
        this.messageSource = dms;
        beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
        if (logger.isTraceEnabled()) {
            logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
        }
    }
}
```
判断beanFactory中是否有名字为messageSource的bean，如果有，从beanFactory中获取并且判断获取的是不是HierarchicalMessageSource类型的，如果是设置其父级消息源
如果没有，新建DelegatingMessageSource类作为messageSource的Bean。

### 8.initApplicationEventMulticaster()
```java
protected void initApplicationEventMulticaster() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
        this.applicationEventMulticaster =
                beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
        if (logger.isTraceEnabled()) {
            logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
        }
    }
    else {
        this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
        if (logger.isTraceEnabled()) {
            logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
                    "[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
        }
    }
}
```
初始化 `ApplicationEventMulticaster` 如果用户自定义了事件广播器，那么使用用户自定义的事件广播器如果用户没有自定义事件广播器，那么使用默认的 
`ApplicationEventMulticaster` 即 `SimpleApplicationEventMulticaster`.

### 9.onRefresh()
```java
protected void onRefresh() throws BeansException {
    // For subclasses: do nothing by default.
}
```
&ensp;&ensp;空方法，Spring 预留给子类需要时来实现。

### 10.registerListeners();
```java
protected void registerListeners() {
    // Register statically specified listeners first.
    // 硬编码的方式注册监听器的处理
    for (ApplicationListener<?> listener : getApplicationListeners()) {
        getApplicationEventMulticaster().addApplicationListener(listener);
    }

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let post-processors apply to them!
    // 配置文件注册监听器的处理
    String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
    for (String listenerBeanName : listenerBeanNames) {
        getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
    }

    // Publish early application events now that we finally have a multicaster...
    Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
    this.earlyApplicationEvents = null;
    if (earlyEventsToProcess != null) {
        for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
            getApplicationEventMulticaster().multicastEvent(earlyEvent);
        }
    }
}
```
&ensp;&ensp;获取容器中的监听器，然后得到当前容器中的 `applicationEventMulticaster`，调用 `addApplicationListener`进行添加。
这里的 `applicationEventMulticaster` 就是刚才初始化的，如果为空就是 `SimpleApplicationEventMulticaster`。

&ensp;&ensp;从BeanFactory中获取ApplicationListener类型的Bean，并且添加为ListenerBeans。

&ensp;&ensp;获取需要提前发布的事件，进行广播。

### 最后
&ensp;&enep;至此， `refresh()`方法中的各个子方法的学习，仅剩了一个`finishBeanFactoryInitialization()`。关于这个方法，涉及到很多新的过程
并且与 `Bean` 的实例化过程息息相关。后面的文章将会介绍 `Bean` 的实例化。这里就是`Bean`的实例化入口的地方。

            

