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