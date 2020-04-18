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
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotationConfigApplicationContext_init_sequence.jpg">
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
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotationConfigApplicationContext_init_sequence.jpg">
 </div>

&enps;&ensp;从上述两幅图片的对比，发现，在`BeanFactoryPostProcessor`的之类中 `getBean()`，方法的执行的时候，
BeanPostProcessor的子类是没有获取到的，那到底是什么原因，造成这种情况的呢？答案只有一个，那就在源码中。。。



