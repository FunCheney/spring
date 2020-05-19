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

### this()

#### 1. AnnotationConfigApplicationContext 的类关系

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/annotionConfigApplication/AnntionConfigApplicationContext_class_relation.jpg">
 </div> 

- a.首先可以看出 AnnotationConfigApplicationContext 在Spring IoC中是属于 ApplicationContext 设计体系下的.
- b.在本文中我们看红色线条表示的部分，来分析其对应的调用逻辑，初始化调用时序图如下：

#### 2. AnnotationConfigApplicationContext 初始化时序图
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/annotionConfigApplication/new_AnnotationConfigApplicationContext.jpg">
 </div> 
 
 &ensp;&ensp;从上图中可以看出，在不同的父类方法构造方法中，完成了不同的初始化，赋予的容器特定的功能：

 - `DefaultResourceLoader` 中通过 `ClassUtils.getDefaultClassLoader()` 完成了 `ClassLoader` 的初始化。 
 -  `AbstractApplicationContext` 中完成了资源加载器的初始化，`PathMatchingResourcePatternResolver()` 完成了 `ResourceLoader
  `初始化，使得容器拥有了加载资源的功能。在初始化的时候 `ResourceLoader` 的时候传入 `this` 代指当前的对象 `AbstractApplicationContext` 
  完成初始化，所有的 IoC 容器的的实现都是 `ResourceLoader` 的子类。
 - `GenericApplicationContext` 中完成了默认 `BeanFactory` 的实现 `DefaultListableBeanFactory` 的初始化，容器默认的实现。

#### 2. new AnnotationConfigApplicationContext() 的调用逻辑
&ensp;&ensp;在调用 `new` 方法的时候，完成类的初始化，先会调用父类的构造方法，对用调用的方法如下：
* ①：`AnnotationConfigApplicationContext#AnnotationConfigApplicationContext(Class<?>... componentClasses
)` 调用有参构造函数，里边做了三件事：
```java
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
    /**
     * 调用默认的构造方法，由于该类有父类，
     * 故而先调用父类的构造方法，在调用自己的构造方法
     * 在自己的构造方法中初始一个读取器和一个扫描器
     */
    this();
    /**
     * 向spring 的容器中注册bean
     */
    register(componentClasses);
    /**
     * 初始化spring的环境
     */
    refresh();
}
```
* ②：`AnnotationConfigApplicationContext#AnnotationConfigApplicationContext()` 有参构造函数里面的第一件事，通过`this()`调用无惨的构成函数
初始化 `reader` 和 `scanner`
```java
public AnnotationConfigApplicationContext() {
    /**
     * 实例化 读取器
     * 将spring中加了注解的类转化为 一个 spring当中bean的描述文件
     */
    this.reader = new AnnotatedBeanDefinitionReader(this);
    /**
     * 实例化 扫描器
     * 能够扫描一个类，并转换为 spring当中bean的描述文件
     * 不通过显示的调用scanner的方法的情况下，spring中的包不是由该scanner扫描
     * 而是由Spring 在实例化 AnnotatedBeanDefinitionReader时
     *   自己new的一个 ClasspathBeanDefinitionScanner 对象扫描完成的
     * 即：这里初始化的扫描器通过程序员显示的调用才会生效。
     */
    this.scanner = new ClassPathBeanDefinitionScanner(this);
}
```

   - 2.1: `AnnotatedBeanDefinitionReader#AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment
    environment)`
   ```java
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
   + 2.1.1: `AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry)`
   ```java
      public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
          registerAnnotationConfigProcessors(registry, null);
      }
    ```
   - 2.2: `ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment
      environment)`
   ```java
       public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
               Environment environment, @Nullable ResourceLoader resourceLoader) {
 
           Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
           this.registry = registry;
 
           if (useDefaultFilters) {
               registerDefaultFilters();
           }
           setEnvironment(environment);
           setResourceLoader(resourceLoader);
       }
   ```
* ③：`GenericApplicationContext#GenericApplicationContext()` 调用父类的构造方法
```java
public GenericApplicationContext() {
    /** 实例化beanFactory */
    this.beanFactory = new DefaultListableBeanFactory();
}
```
   - 3.1：`DefaultListableBeanFactory#DefaultListableBeanFactory()`
   ```java
    public DefaultListableBeanFactory() {
          /** 调用父类的构造方法*/
          super();
    }
   ```
* ④：`AbstractApplicationContext#AbstractApplicationContext()` 调用父类的构造方法
```java
public AbstractApplicationContext() {
    // 这里根据不同的实现调用 不同的初始化方法
    this.resourcePatternResolver = getResourcePatternResolver();
}
```
   - 4.1：`AbstractApplicationContext#getResourcePatternResolver()`
   ```java
        protected ResourcePatternResolver getResourcePatternResolver() {
            // 这里的 this 是指 AbstractApplicationContext 它是 DefaultResourceLoader 的子类
           return new PathMatchingResourcePatternResolver(this);
        }
   ```
   + 4.1.1：`PathMatchingResourcePatternResolver#PathMatchingResourcePatternResolver()`
   ```java
         public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
             Assert.notNull(resourceLoader, "ResourceLoader must not be null");
             // 若指定了 ResourceLoader 则使用指定的
              this.resourceLoader = resourceLoader;
         }
   ```
* ⑤：`DefaultResourceLoader#DefaultResourceLoader()`调用父类的构造方法
```java
public DefaultResourceLoader() {
    this.classLoader = ClassUtils.getDefaultClassLoader();
}
```
- 5.1：`ClassUtils.getDefaultClassLoader()`


#### 3. 调用过程时序图

 <div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/annotionConfigApplication/AnnotationConfigApplicationContext_init_sequence.jpg">
 </div>

&ensp;&ensp;上述时序图中涉及资源加载器的初始化，DefaultListableBeanFactory 的初始化，以及AnnotatedBeanDefinitionReader
和ClassPathBeanDefinitionScanner的初始化化，其中AnnotatedBeanDefinitionReader涉及的东西较多，需要一点一点详细的分析。

### new AnnotatedBeanDefinitionReader(this)
&ensp;&ensp;下面，请坐稳了，继续开始发车。。。让我们进入AnnotatedBeanDefinitionReader的得初始化过程！！！

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


