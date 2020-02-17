## BeanFactory
&ensp;&ensp;在BeanFactory中提供了最基本的IoC容器的功能，定义了IoC容器最基本的形式，并且提供了IoC
容器所应该遵循的最基本的原则。在Spring中，BeanFactory只是一个接口类，对IoC的基本功能做了封装。其实现
类，如DefaultListableBeanFactory、XmlBeanFactory、ApplicationContext等都可以看成是IoC容器
附加某种功能的具体实现。

### BeanFactory 接口的定义

```java
public interface BeanFactory {

	/**
	 * 使用转义符"&"来得到 FactoryBean。用来区分通过容器获取 FactoryBean 产生的对象，和 FactoryBean 本身
	 * 举例：
	 *   myJndiObject  得到一个 FactoryBean 的对象
	 *   &myJndiObject 得到的是 FactoryBean 的工厂，用来产生 FactoryBean 的对象
	 * 区分 FactoryBean 和 BeanFactory
	 *   FactoryBean 是对象，是一个能产生或者修饰对象生成的工厂 Bean
	 *   BeanFactory 是对象工厂 也是就 IOC 容器，所有的 Bean 都是由 BeanFactory 进行管理
	 */
	String FACTORY_BEAN_PREFIX = "&";


	/**
	 * 
	 */
	Object getBean(String name) throws BeansException;

	/**
	 * 
	 */
	<T> T getBean(String name, Class<T> requiredType) throws BeansException;

	/**
	 *
	 */
	Object getBean(String name, Object... args) throws BeansException;

	/**
	 *
	 */
	<T> T getBean(Class<T> requiredType) throws BeansException;

	/**
	 *
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

	/**
	 *
	 */
	<T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

	/**
	 *
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
	 * 
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
