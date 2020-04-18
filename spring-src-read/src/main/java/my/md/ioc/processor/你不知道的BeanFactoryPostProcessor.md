### BeanFactoryPostProcessor

```java
@Component
public class MyFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		int count = beanFactory.getBeanDefinitionCount();
		String[] names = beanFactory.getBeanDefinitionNames();
		System.out.println("当前BeanFactory中有"+count+" 个Bean");
		System.out.println(Arrays.asList(names));
		MyService bean = beanFactory.getBean(MyService.class);
		System.out.println(bean);
	}
}
```
&ensp;&ensp;上述代码中的getBean()方法在执行的过程中关于 BeanPostProcessor 的处理：

 <div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/processor/BeanFactoryProcessor%E4%B8%8E%E6%AD%A3%E5%B8%B8getBean%E5%AF%B9%E6%AF%94_01.png">
 </div>

 &ensp;&ensp;下面我们在对正常情况下getBean()方法中，关于 BeanPostProcessor的处理，代码示例，与断点调试如下：

 在容器中getBean()代码示例：
 ```java
ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
MyService myService = ann.getBean(MyService.class);
myService.test();
 ```
 断点调试结果如下：
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/processor/BeanFactoryProcessor%E4%B8%8E%E6%AD%A3%E5%B8%B8getBean%E5%AF%B9%E6%AF%94_02.png">
 </div>

&enps;&ensp;从上述两幅图片的对比，发现，在`BeanFactoryPostProcessor`的之类中 `getBean()`，方法的执行的时候，
BeanPostProcessor的子类是没有获取到的，那到底是什么原因，造成这种情况的呢？答案只有一个，那就在源码中。。。

程序的入口是：AbstractApplicationContext#refresh() 方法，这个方法过于复杂，这里我们只看与上述问题相关的部分，
找的关于BeanFactoryPostProcessor相关的处理代码：

`invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
` 这一行代码，对 Spring 内置的后置处理器`ConfigurationClassPostProcessor`进行处理。  
这里会完成扫描，将我们交给Spring管理的对象注册到 beanDefinitionMap 中。

`invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);`这里执行的是 BeanFactoryPostProcessor 的子  
类 BeanDefinitionRegistryPostProcessor 的回调方法。同样在这里会处理 ConfigurationClassPostProcessor 的 postProcessBeanFactory方法，
最后通过 `beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));`这里就是图片①中的三个后置处理器的添加，最终结果如下

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/processor/BeanFactoryProcessor%E4%B8%8E%E6%AD%A3%E5%B8%B8getBean%E5%AF%B9%E6%AF%94_03.png">
 </div>

`invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);`这里执行的是 BeanFactoryPostProcessor 的 postProcessBeanFactory() 方法,
也就是说，我们定义的BeanFactoryPostProcessors的子类中的方法会在这里执行。如果在这里getBean()执行的时候，会获取通过 getBeanPostProcessors()，获取Bean的后置处理器。
这里已我这里的 beanName 为 'myService' 为例，只有三个 Bean的后置处理器。

然后是refresh() 方法中的 `registerBeanPostProcessors(beanFactory);` 这里注册 Bean的后置处理器。

1. 首先添加一个默认的
2. 获取内置的 org.springframework.context.annotation.internalAutowiredAnnotationProcessor
3. 获取内置的 org.springframework.context.annotation.internalCommonAnnotationProcessor

第一个通过 beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));
的方式添加到 beanPostProcessors 中。

后面两个通过 registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors)
的方式添加到 beanPostProcessors 中。

`private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();` 这个是Spring IoC
中用来存放 BeanPostProcessor 的。

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/processor/BeanFactoryProcessor%E4%B8%8E%E6%AD%A3%E5%B8%B8getBean%E5%AF%B9%E6%AF%94_04.png">
</div>

&ensp;&ensp;其中 `AutowiredAnnotationBeanPostProcessor` 涉及对 @Autowired 的处理。`CommonAnnotationBeanPostProcessor`涉及对
@PostConstruct  @PreDestroy 注解测处理。

&ensp;&ensp;综上，如果在 BeanFactoryPostProcessor 的子类极其实现中，getBean()时，对于 BeanPostProcessor 的实现类还没有
添加到 BeanFactory 中的 beanPostProcessors 中去。因此在 BeanFactoryPostProcessor 中 getBean() 会破坏 bean的封装，有些属性  
不会获取到。因此，在 BeanFactoryPostProcessor 的子类中你不建议去创建 Bean，而只是完成对某些bean的属性修改。








