## BeanFactory
&ensp;&ensp;在BeanFactory中提供了最基本的IoC容器的功能，定义了IoC容器最基本的形式，并且提供了IoC
容器所应该遵循的最基本的原则。在Spring中，BeanFactory只是一个接口类，对IoC的基本功能做了封装。其实现
类，如DefaultListableBeanFactory、XmlBeanFactory、ApplicationContext等都可以看成是IoC容器
附加某种功能的具体实现。

### BeanFactory 接口的定义

```java
public interface BeanFactory {

	/**
	 * 使用转义符"&"来得到 FactoryBean本身。用来区分通过容器获取 FactoryBean 产生的对象，和 FactoryBean 本身
	 * 举例：
	 *   myObject  得到一个 FactoryBean (产生)修饰的对象
	 *   &myObject 得到的是 FactoryBean 的工厂，用来产生 FactoryBean 的对象
	 * 区分 FactoryBean 和 BeanFactory
	 *   FactoryBean 是对象，是一个能产生或者修饰对象生成的工厂 Bean
	 *   BeanFactory 是对象工厂 也是就 IOC 容器，所有的 Bean 都是由 BeanFactory 进行管理
	 */
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * 通过指定的名字获取Bean
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 通过Bean的名称 和 类型 获取 Bean
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 * 通过Bean的名称 和 构造参数 获取 Bean
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 * 通过Bean的类型
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 * 通过Bean的类型 和 构造参数
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 * 方法用于获取指定bean的提供者，可以看到它返回的是一个ObjectProvider，其父级接口是ObjectFactory
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 * 通过ResolvableType获取ObjectProvider
	 */
	<T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

	/**
	 * 通过指定的名字判断 工厂中是否定义 或者 已经注册了 一个 BeanDefinition
	 */
	boolean containsBean(String name);

	/**
	 * 用来出查询指定名字的 bean 是否是单例（singleton）的，单例属性 是在 BeanDefinition 指定的
	 */
	boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 用来出查询指定名字的 bean 是否是原型（prototype）的，原型属性 是在 BeanDefinition 指定的
	 */
	boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 判断指定名字的 Bean 的 Class 类型是否是特定的 Class 类型
	 */
	boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 判断指定名字的 Bean 的 Class 类型是否是特定的 Class 类型
	 */
	boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

	/**
	 * 查询指定名字的 Bean 的 Class 类型
	 */
	@Nullable
	Class<?> getType(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 获取指定的名字的 Bean 的所有别名，别名是用户在 BeanDefinition 中自己定义的
	 */
	String[] getAliases(String name);

}
```
&ensp;&ensp;BeanFactory接口设计了getBean() 方法，这个方法是IoC容器API的主要方法，通过这个方法，
可以取得IoC容器中管理的Bean，Bean的取得是通过指定的名字来索引的。如果需要在获取Bean的时候对Bean的类型
进行检查，BeanFactory接口定义了带有参数的getBean()方法，这个方法的使用与不带参数的方式类似，不同的是增
加了对Bean检索的类型的要求。



#### 区分BeanFactory 和 FactoryBean

* BeanFactory 是一个工厂，IoC容器。
* FactoryBean 是一个Bean。在Spring中，所有的Bean都是右BeanFactory(IOC容器)来进行管理的。但是
对于FactoryBean 而言，这个Bean不是简单的Bean，而是一个能产生或者修饰对象生成的工厂Bean。


