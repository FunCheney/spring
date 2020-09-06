>作者：零星。（微信公众号ID：Fchen），欢迎分享，转载请保留出处。

&ensp;&ensp;说起 `Spring` 的扩展点，大家都会想到 `BeanPostProcessor` 和 `BeanFactoryPostProcessor` 这两个处理器，并给他们起名对象的后置处理器和工厂的后置处理器。

&ensp;&ensp;对于这两个后置处理器相关的分析，其实在前面的文章中都已经或多或少的有提及了。但是，没有办法，这两后置处理器确实在 `Spring` 中有着非常重要的地位。你或许会说，我平常在开发的时候好像没有自己去写类似于这样的扩展点，去实现里面的方法，来插手 `Bean` 工厂初始化、影响 `Bean`的生命周期，等等。但是在 `Spring` 的内部，我们所用到的注解，基本上都是通过这两个扩展点来完成的。

&ensp;&ensp;不知道你是否还记得 `@Configuration` 这个注解的处理类 `ConfigurationClassPostProcessor`。可以这么说，纵观整个 `Spring` 对于 `BeanFactoryPostProcessor` 实现的类中，这个是最最最重要的。应为有了这个类，我们自定义的被 `@Configuration` 注解的类才得以被处理。我们定义的对象，才有机会被加入到 `IoC` 容器中。

&ensp;&ensp;不知道你是否还记得 `@Autowired` 注解的处理类 `AutowiredAnnotationBeanPostProcessor`。在这个类中，对被注入的对象做了依赖注入，完成了对象之间依赖关系的建立。当然还有其他的注解的处理，基本上都是 `BeanPostProcessor` 的子类。

&ensp;&ensp;今天这篇文章我试着对比一下这两个扩展点。


## 1.相同点

### 1.1设计思想

&ensp;&ensp;首先 `BeanFactoryPostProcessor` 和 `BeanPostProcessor` 都是以接口的形式定义了方法，供子类实现，来完成自己想要做的情。

&ensp;&ensp;`BeanPostProcessor` 是 `Spring` 框架提供的一个类的扩展点，称之为 `bean`的后置处理器。通过实现 `BeanPostProcessor` 接口，可以干预 `bean` 的初始化过程，从而减轻了 `BeanFactory` 负担。
```java
public interface BeanPostProcessor {

	/**
	 * 在bean的初始化之前执行
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * 在bean的初始化之后执行
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
```
&ensp;&ensp; `BeanFactoryPostProcessor` 用来插手 `BeanFactory`的初始化。影响容器的形成。以 `ConfigurationClassPostProcessor` 为例，在该子类的实现中，完成了对 `@Configuration`注解类的解析。
```
public interface BeanFactoryPostProcessor {

	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
```
&ensp;&ensp;在 `ConfigurationClassPostProcessor` 类中通过实现上述的方法，完成了对配置类的动态代理。

### 1.2 在容器中的存储方式
&ensp;&ensp;由于 `BeanFactoryPostProcessor` 和 `BeanPostProcessor` 都是已接口的方式被定义在容器中，因此，在 `IoC` 容器中，定义了两个集合来存放。

```java
private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
```
&ensp;&ensp;通过 `beanPostProcessors` 来存放 `BeanPostProcessor` 的子类。当你想在容器中获取这个集合时，通过下面的方法便可得到：
```java
public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}
```
&ensp;&ensp;对于 `BeanFactoryPostProcessor` 的子类也是类似的，在 `Spring` 中将其存放在：`beanFactoryPostProcessors` 中。
```java
private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
```
&ensp;&ensp;通过 `getBeanFactoryPostProcessors()` 方法来获取：
```
public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
      return this.beanFactoryPostProcessors;
  }
```
&ensp;&ensp;这里这样说，应该是不太严谨的，因为 `getBeanFactoryPostProcessors()` 获取到的是程序员自己定义且没有交给 `Spring` 管理的 `BeanFactoryPostProcessor` 的子类。这里就不在赘述了，可以查看：[invokeBeanFactoryPostProcessors()方法的处理](!https://juejin.im/post/6844904175512338446)里面有详细的解释。

### 1.3 子类的处理方式

&ensp;&ensp;对于实现了这两个类的子类，在容器中会存在一批。对于每个子类的处理，都是通过 `for` 循环这一批子类，来执行每个子类所对应的方法。

![BeanFactoryPostProcessor的子类的处理方式](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/51747bf88e214ecdacb342234c0180be~tplv-k3u1fbpfcp-zoom-1.image)

![BeanPostProcessor子类的处理方式](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/96b94069148648b6aa93d8c4efaebc15~tplv-k3u1fbpfcp-zoom-1.image)

&ensp;&ensp;可以看出，这两个类子类的处理方式都是相同的处理方式，在容器初始化的时候，将他们添加到对应的变量中存起来。后面要用到的时候拿出来，然后在循环处理。

## 2.不同点
### 2.1 面向的对象不同
&ensp;&ensp;见名知义，这两个对象，一个是面对 `BeanFactory` 搞事情的。一个是面对 `Bean` 搞事情的？是不是觉得有点抽象？(**试着去感受他，而不是理解他，我承认我被《信条》带偏了，原谅我智商不够，我没看懂。需要补的知识点有点多。。。还是学习吧！**)没事，这个我想我应该还是可以解释清楚的。

&ensp;&ensp;要分析 `BeanFactoryPostProcessor` 是面对 `BeanFactory` 的。这个还是要存容器的 `refresh()` 方法说起了。在执行 `refresh()` 方法之前，我们自己定义的对象注册的 `BeanDefinitionMap` 中的只有一个，那就是我们的**配置类**。在完成 `invokeBeanFactoryPostProcessors(beanFactory)` 后，容器中的 `BeanDefinitionMap` 包含了我们开发中自己定义的需要交给 `Spring` 管理的对象。这写处理都是在`ConfigurationClassPostProcessor`类中来完成的。这样才使得我们的容器中的内容更加丰富了。当你的容器中有了 `BD（BeanDefinition）`才为后面有关 `getBea()` 的操作提供了可能。然后还要指出一点，对于加了 `@Configuration`的类，在其 `BD` 中会被打标为 `Full` 模式，最后通过 `CGLIB` 生成动态代理的对象。

&ensp;&ensp;要分析 `BeanPostProcessor` 是面对 `Bean` 的。这个我选一个在开发中经用到的注解 `@Autowired` 来说起。`@Autowired` 的设计，就是用来完成对象的属性注入的。对与对象之间依赖关系的处理是在 `org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw)` 这个方法中来处理的。在这个方法中有一段代码上面图片（BeanPostProcessor子类的处理方式）所示，判断后置处理器的类型然后去执行方法。

&ensp;&ensp;在 `AutowiredAnnotationBeanPostProcessor` 中有通过如下一段代码开始了对象之间的依赖注入，其中 `findAutowiringMetadata()` 获取到当前对象中所有被注入的属性，然后做了相应的处理。这里不在赘述依赖注入的过程，感兴趣请查看[Spring 注入对象处理过程](!https://juejin.im/post/6854573218277048328)

```java
public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
    try {
        // 注入
        metadata.inject(bean, beanName, pvs);
    }
    catch (BeanCreationException ex) {
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
    }
    return pvs;
}
```
&ensp;&ensp;通过上述的分析，应该可以理解 `BeanFactoryPostProcessor` 是面对 `BeanFactory` 搞事；`BeanPostProcessor` 是面对 `Bean` 搞事情的吧？

### 2.2 通过代码来理解一下
&ensp;&ensp;如果还是不太理解上面所说面向对象的不同，我们通过下面的例子来看一下，首先我们自定义一个 `BeanFactoryPostProcessor` 的实现类，在期子类实现中，我们通过 `getBean()` 来获取对象，然后调用对象的 `test()` 方法。
```
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
		bean.test();
	}
}
```
&ensp;&ensp;定义两个类，`MyService` 与 `MyTest`。起中讲 `MyTest` 注入给 `MyService`。然后调用 `MyTest` 的 `test` 方法。
```
@Service
public class MyService {
	@Autowired
	MyTest myTest;

	public void test(){
		System.out.println("hello test");
		myTest.test();
	}
}
```
&ensp;&ensp;代码如上所述，你猜运行结果是什么？调试运行结果如下：

![示例结果](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e9acb812cdd6408d96746cc83ecae6fc~tplv-k3u1fbpfcp-zoom-1.image)

&ensp;&ensp;我将上述的结果分成了三部分，第一部分说明 `BeanDefinitionMap`中已经包含了我们容器中所有的对象，因为对应的 `@Configuration` 注解的类被处理完了。

&ensp;&ensp;第二部分，在这里 `getBean()` 可以触发 `Bean` 的实例化等一系列的操作。

&ensp;&ensp;第三部分，抛出空指针，说明依赖的对象没有注入进来。纠其原因，说明在这个时候，`BeanFactoryPostProcessor` 的子类已经插手了容器的初始化，影响到了容器中的对象。但是对象的后置处理器还没有添加到容器中。前面说了 `@Autowired` 是通过对象的后置处理器来完成属性注入的，导致对应的属性没有被注入到对象中。

&ensp;&ensp;综上所述我想：**`BeanFactoryPostProcessor` 是面对 `BeanFactory` 搞事；`BeanPostProcessor` 是面对 `Bean` 搞事情。** 这个我应该解释清楚了吧。

## 3.总结
&ensp;&ensp;这篇文章对比分析了 `Spring` 的扩展点 `BeanFactoryPostProcessor` 和 `BeanPostProcessor` 的异同。通篇就是为了说明一个事情：**`BeanFactoryPostProcessor` 是面对 `BeanFactory` 搞事；`BeanPostProcessor` 是面对 `Bean` 搞事情。**
当然，在 `BeanFactoryPostProcessor` 也可完成对对象属性的设置。这里就不在做更详细的区分了。