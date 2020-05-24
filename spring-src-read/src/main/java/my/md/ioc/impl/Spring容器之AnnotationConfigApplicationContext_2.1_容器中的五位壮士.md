## Spring IoC 初始化之先发五虎 
&ensp;&ensp;接上文所述，源码分析来到了 `AnnotationConfigApplicationContext()` 方法中的 `new AnnotatedBeanDefinitionReader(this)`
 方法。下面就从 `new AnnotatedBeanDefinitionReader(this)`这一行代码开始。首先解释`this`，这里`this`指的是当前类，也即 AnnotationConfigApplicationContext。
### 1. 代码入口
```java
/**
 * BeanDefinitionRegistry是通过AnnotatedBeanDefinitionReader构造方法中的this传进来
 * 这里说明 AnnotationConfigApplicationContext 就是 BeanDefinitionRegistry。
 */
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this(registry, getOrCreateEnvironment(registry));
}
```
```java
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    Assert.notNull(environment, "Environment must not be null");
    this.registry = registry;
    this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
    /**
     * 通过AnnotationConfigUtils.registerAnnotationConfigProcessors()
     * 获取所有BeanPostProcessor 的bean
     */
    AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
}
```
&ensp;&ensp;看到这里，你可能觉得篇文章的题目是在吸引眼球，到目前为止五壮士的一点毛信息都没有。请不要着急，请相信，我们一定会一点一点的揭开这五位壮士的神秘面纱的。来不及了，
快上车。。。

&ensp;&ensp; `registerAnnotationConfigProcessors` 中通过给定的注册器，注册所有注解相关的后置处理器。
```java
public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
    registerAnnotationConfigProcessors(registry, null);
}
```
```java
public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
        BeanDefinitionRegistry registry, @Nullable Object source) {
    /**
     * 通过registry生成一个beanFactory
     */
    DefaultListableBeanFactory beanFactory = unwrapDefaultListableBeanFactory(registry);
    if (beanFactory != null) {
        if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
            /**
             * 添加AnnotationAwareOrderComparator类的对象，注意去排序
             */
            beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
        }
        if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
            /**
             * 提供处理延迟加载的功能
             */
            beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
        }
    }

    Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);
    /** 注册BeanDefinition，到Map中*/
    if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        /**
         * ConfigurationClassPostProcessor 的类型是 BeanDefinitionRegistryPostProcessor
         * BeanDefinitionRegistryPostProcessor 实现的是 BeanFactoryPostProcessor 接口
         *
         * RootBeanDefinition 是 BeanDefinition的子类
         */
        RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    // Check for JSR-250 support, and if present add the CommonAnnotationBeanPostProcessor.
    if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    // Check for JPA support, and if present add the PersistenceAnnotationBeanPostProcessor.
    if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition();
        try {
            def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME,
                    AnnotationConfigUtils.class.getClassLoader()));
        }
        catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Cannot load optional framework class: " + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
        }
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(EventListenerMethodProcessor.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME));
    }

    if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
        RootBeanDefinition def = new RootBeanDefinition(DefaultEventListenerFactory.class);
        def.setSource(source);
        beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME));
    }

    return beanDefs;
}
``` 
&ensp;&ensp;在上述的方法中，都通过 `registerPostProcessor` 中调用 `registerBeanDefinition` 方法将 `definition` 放置到 `Map`当中。可以看到放到`Map`中的这个对象都是Spring内部自己定义的。这说明在容器初始化的时候，加载程序员自定义的`Bean` 的时候，已经有一部分对象呗加载到容器中了，通过这些对象来完成后续其他对象的加载。
```java
/**
 * 往BeanDefinitionMap中注册
 * @param registry
 * @param definition
 * @param beanName
 * @return 返回一个 BeanDefinitionHolder 对象
 */
private static BeanDefinitionHolder registerPostProcessor(
        BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName) {

    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    /**
     * 将bean放入Map中
     */
    registry.registerBeanDefinition(beanName, definition);
    // 返回 BeanDefinitionHolder
    return new BeanDefinitionHolder(definition, beanName);
}
```
&ensp;&ensp;关于 `registerBeanDefinition` 后面在Spring `Bean` 的注册中会详细介绍，这里暂不做介绍。但是这看到有一个 `BeanDefinitionHolder`
对象。从上述代码来看 `BeanDefinitionHolder` 是对 `BeanDefinition` 和 `beanName` 的封装。

&ensp;&ensp; `BeanDefinition` 是 Spring 中对 基于注解，或者通过`xml`方式定义的Bean的封装。这里只做概念上的描述，后续会对 `BeanDefinition`
有一个系统性的认识。


#### 1. 时序图 

 <div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotatedBeanDefinitionReader_init_sequence.jpg">
 </div>

&ensp;&ensp;在上述的时序图中，提到有一个极其重要的过程，在此过程中向IoC容器中的beanDefinitionMap中put了
5个Spring内置的对象，这五个对象对应的Spring 的Bean的描述文件为RootBeanDefinition。

&ensp;&ensp;这些类的实现方式都是对Spring后置处理器的应用，关于Spring的扩展点以及扩展点的应用，在以后都会介绍到。
这里我们先抓住主要矛盾，**容器的初始化**。这5个对象在Spring中对应的常量，对应Spring中的类，以及注解。其对应关系如下:

| 常量  | 对应的BeanPostProcessor	| 对应的注解	| 
|---|---|---|
|CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME| ConfigurationClassPostProcessor | @Configuration|
|AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME| AutowiredAnnotationBeanPostProcessor | @AutoWired |
|REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME | RequiredAnnotationBeanPostProcessor	| @Required |
|COMMON_ANNOTATION_PROCESSOR_BEAN_NAME| CommonAnnotationBeanPostProcessor | @PostConstruct  @PreDestroy |
|EVENT_LISTENER_PROCESSOR_BEAN_NAME| EventListenerMethodProcessor | @EventListener |
|EVENT_LISTENER_FACTORY_BEAN_NAME| EventListenerFactory | EventListener |

&ensp;&ensp;这里要对`ConfigurationClassPostProcessor`这个类要足够的重视，因为该类对应着对注解`@Configuration`的处理。在这里要记住
这个类及其的重要，后面会多次提到这个类。

&ensp;&ensp;至此，上述`涉及代码`中的`第①部分！！！`对应的`this()`方法部分已经，全部结束咯...

#### 容器形成图

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/annotionConfigApplication/spring_ioc_contains_1.jpg">
 </div>