
### FactoryBean 的使用
&ensp;&ensp;一般情况下，Spring通过反射机制利用 bean 的 class 属性指定类来实例化bean。在某些情况下，实例
话 bean 比较复杂，如果按照传统的方式，则需要在`<bean>`中提供大量的配置信息，配置方式的灵活性是受限的，这时采用
编码的方式可能会得到一个简单的方案。Spring为此提供了一个 `FactoryBean`的工厂类接口，用户可以通过实现该接口定制
实例化bean的逻辑。

&ensp;&ensp;`FactoryBean` 接口对于Spring 框架来说占有重要的地位，在Spring内部有很多的`FactoryBean`的实现。
它们隐藏了实例化一些复杂bean的细节，给上层应用带来了便利。

```java
public interface FactoryBean<T> {

	/**
	 * 返回由FactoryBean创建的bean实例，如果 isSingleton返回true，
	 * 则该实例会放置到spring容器中单实例缓存池中
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * 返回FactoryBean 创建的实例Bean的类型
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * 用来判断有FactoryBean创建的是bean的作用域
	 */
	default boolean isSingleton() {
		return true;
	}

}

```

&ensp;&ensp;当配置文件中<bean>的class属性配置的实现类是 `FactoryBean` 时通过 `getBean()`的方法
返回的不是 `FactoryBean` 本身，而是 FactoryBean#getObject()方法所返回的对象，相当于FactoryBean#getObject()
代理了getBean() 方法。

首先定义一个Bean，已user为例
```java
public class User {

	private String name;

	private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
```
然后通过FactoryBean的方式获取

```java
@Component
public class FactoryBeanTest implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		return new User();
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}

```
通过以下方式获取容器中的Bean

```java
public class MyTest {
	public static void main(String[] args) {
		ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		// 方式一
		FactoryBeanTest beanTest = ann.getBean(FactoryBeanTest.class);
		System.out.println(beanTest);
		// 方式二
		Object user = ann.getBean("factoryBeanTest");
		System.out.println(user);
		// 方式三
		Object user2 = ann.getBean("&factoryBeanTest");
		System.out.println(user2);
	}
}
```
输出结果：
```
com.fchen.bean.biz.FactoryBeanTest@55a1c291
com.fchen.bean.biz.User@2145433b
com.fchen.bean.biz.FactoryBeanTest@55a1c291
```
&ensp;&ensp;我们可以看到通过方式一，与方式三获取的是`FactoryBeanTest`的实例，断点调试发现，在获取Bean时，
`beanName`是以"&"开始的，如下图：
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/bean/Spring%E4%B9%8BFactoryBean%E7%9A%84%E8%8E%B7%E5%8F%96.png">
 </div>

&ensp;&ensp;通过方式二，`ann.getBean("factoryBeanTest")`时，Spring通过反射机制发现 FactoryBeanTest 实现了
`FactoryBean接口`，这时Spring中的容器就调用接口方法FactoryBeanTest#getObject()，或的对象。



