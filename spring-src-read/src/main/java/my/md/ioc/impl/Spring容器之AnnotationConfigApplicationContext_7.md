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
### 7.initMessageSource()


### 8.initApplicationEventMulticaster()

### 9.onRefresh()

### 10.registerListeners();



