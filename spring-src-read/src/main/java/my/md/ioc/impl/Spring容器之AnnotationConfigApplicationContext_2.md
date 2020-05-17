## AnnotationConfigApplicationContext 源码解析
&ensp;&ensp;Spring IoC容器的初初始化过程中，无论是基于XML还是基于注解，都包含BeanDefinition的定位、
加载、注册三个过程。本文通过这三个过程，来分析IoC容器的初始化。

&ensp;&ensp;基于注解的IoC容器的使用，见上一篇文章。这里分析源码，入口代码 `new AnnotationConfigApplicationContext(XXX)`，
通过这行代码，开始我们的晕车之旅。。。

涉及代码如下：
```java
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

### new AnnotationConfigApplicationContext() 做了什么

#### 1. AnnotationConfigApplicationContext 的类关系

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnntionConfigApplicationContext_class_relation.jpg">
 </div> 

- a.首先可以看出 AnnotationConfigApplicationContext 在Spring IoC中是属于 ApplicationContext 设计体系下的，
- b.在本文中我们看红色线条表示的部分，来分析其对应的调用逻辑

#### 2. new AnnotationConfigApplicationContext() 的调用逻辑
- ①：AnnotationConfigApplicationContext#AnnotationConfigApplicationContext(Class<?>... componentClasses)
- ②：AnnotationConfigApplicationContext#AnnotationConfigApplicationContext()
- ③：GenericApplicationContext#GenericApplicationContext()
- ④：AbstractApplicationContext#AbstractApplicationContext()
- ⑤：AbstractApplicationContext#getResourcePatternResolver()
- ⑥：PathMatchingResourcePatternResolver#PathMatchingResourcePatternResolver()
- ⑦：DefaultResourceLoader#DefaultResourceLoader()
- ⑧：ClassUtils.getDefaultClassLoader()
- ⑨：DefaultListableBeanFactory#DefaultListableBeanFactory()
- ⑩: AnnotatedBeanDefinitionReader#AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment)
- ⑪: AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry)
- ⑫: ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment)
			
&ensp;&ensp;在调用AnnotationConfigApplicationContext的无惨构造方法的时候，有与该类有父类所以
首先调用父类的构造方法，然后在调用自己的构造方法。

&ensp;&ensp;在上述的步骤中在第⑥ ~ ⑧中，可以看到，是获取资源加载器ResourceLoader的过程；然后第⑨步，
是实例化IoC容器的过程，这里也是默认实现 DefaultListableBeanFactory，期间也包含一些对IoC容器的操作，
如ignoreDependencyInterface()的功能，忽略给定接口的自动装配功能；再然后第⑩、⑪是实例化被注解的类对应
在Spring中的Bean的描述文件BeanDefinition的读取器 AnnotatedBeanDefinitionReader 的过程，*(这部分
比较复杂，后续专门学习)*。最后，第⑫步是实例化扫描器 ClassPathBeanDefinitionScanner 的过程，该扫描器
能够扫描一个类，并转换为 spring当中bean的描述文件。这里要注意，这个扫描器是Spring显示的提供出来，供开发
人员使用的。

#### 3. 时序图

 <div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotationConfigApplicationContext_init_sequence.jpg">
 </div>

&ensp;&ensp;上述时序图中涉及资源加载器的初始化，DefaultListableBeanFactory 的初始化，以及AnnotatedBeanDefinitionReader
和ClassPathBeanDefinitionScanner的初始化化，其中AnnotatedBeanDefinitionReader涉及的东西较多，需要一点一点详细的分析。

### new AnnotatedBeanDefinitionReader(this)
&ensp;&ensp;下面，请坐稳了，继续开始发车。。。让我们进入AnnotatedBeanDefinitionReade的得初始化过程！！！

&ensp;&ensp;就从 `new AnnotatedBeanDefinitionReader(this)`这一行代码开始。首先解释this，这里this指的
是当前类，也即 AnnotationConfigApplicationContext。从类图得知，该类实现了 BeanDefinitionRegistry，可以
看出这个IoC容器也是一个Registry。

#### 1. 时序图 

 <div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotatedBeanDefinitionReader_init_sequence.jpg">
 </div>

&ensp;&ensp;在上述的时序图中，提到有一个极其重要的过程，在此过程中向IoC容器中的beanDefinitionMap中put了
5个Spring内置的对象，这五个对象对应的Spring 的Bean的描述文件为RootBeanDefinition。

&ensp;&ensp;这5个对象在Spring中对应的常量，对应Spring中的类，以及注解。其对应关系如下:

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


