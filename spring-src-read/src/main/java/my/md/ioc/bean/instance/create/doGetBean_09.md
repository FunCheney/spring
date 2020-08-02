> 我们终此一生，就是要摆脱别人的期待，找到真正的自己。
                                       --《无声告白》
## 循环依赖与解决循环依赖
&ensp;&ensp;上一篇文章中系统的了解了 `Spring` 关于属性注入的处理，详细分析了 `@Autowired` 注解字段，以及简单
分析了 `@Autowired` 注解构造方法的处理方式。我们知道在使用 `Spring` 的时候，如果应用设计比较复杂，那么在这个应用
中， `Ioc` 管理的 `Bean` 的个数可能非常多，这些 `Bean` 之间的以依赖关系也会非常复杂。这个时候就有可能会出现容器
中的 `Bean` 相互引用的情况。

### 1. 什么是循环依赖
&ensp;&ensp;循环依赖是指：对象的之间互相持有对方的引用，最终形成闭环。我们通过下面的图，先来简单的了解一下，然后在分情况演示：
![](https://imgkr.cn-bj.ufileos.com/b3962486-b878-450a-b522-696ab387352a.jpg)

&ensp;&ensp;看到上述的这幅图，我不由的想起了一道简单的算法题：**如何判断链表有环？** 大家可以简单的思考一下。
#### 1.1 对象之间的持有方式
**情形一：** 对象持有自己，造成循环

```java
public class Student {
    private Student A = new Student();
}
```
**结果：**

![对象中的属性是自己](https://imgkr.cn-bj.ufileos.com/1c90d24b-770e-4051-9fba-f86e38c927eb.jpg)

**情形二：** 两个对象之间互相持有，造成循环
```java
public class School {
    private Student A  = new Student();
}
```

```java
public class Student {
    private School A = new School();
}
```

**结果：**
![两个对象之间互相持有](https://imgkr.cn-bj.ufileos.com/b539f5a4-495d-4f30-9e4b-7ed8aa8fccae.jpg)


**情形三：** 多个对象之间的循环引用，造成循环
&ensp;&ensp;多个对象相互引用造成循环依赖，这里不在进行代码举例，我们可以猜到，在这种情况下会造成相同的异常结果。

&ensp;&ensp;通过上面的描述，我们发现在 `java`对象中，如果对象之间的属性存在循环引用的情况，也就是说 `A` -> `B` -> `C` ... -> `A`那么在对象初始化的时候会抛出 `java.lang.StackOverflowError`。

### 2. 在使用 Spring 过程中的循环依赖
&ensp;&ensp;上一篇文章[注入对象的处理过程](https://juejin.im/post/5f1b08916fb9a07ead0f677a)中说到，关于 Spring 中处理属性注入处理过程。可以看到 当在 A 对象中注入 B 对象，然后通过 `Bean` 的后置处理器来
处理，最后通过调用 `getBean()` 方法来完成属性的注入。这篇文章将接着上一篇，分析更为复杂的部分，循环依赖。首先还是通过使用场景以及有可能
出现的异常情况来开始，然后在通过源码分析。

&ensp;&ensp;正式开始之前，还是要做一个简单的回顾，因为很多时候，刚学习到脑子里的知识吃一顿饭，就忘了。何况已经吃了这么多顿饭了。。。

&ensp;&ensp;我想，说起 `Bean` 的实例化，就应该知道在 `Spring` 中有一个非常重要的方法 `doCreateBean()`，这个方法也是在前面的几篇文章中重点分析的方法。我们知道
`createBeanInstance()` 方法返回 `BeanWrapper` 对象；`populateBean()` 方法处理对象之间的依赖关系；`initializeBean()` 完成对 `Bean`的处理（后续的文章中会继续分析）。

&ensp;&ensp;在上一篇文章中，我们分析了 `@Autowiued` 注解构造器以及注解属性的方式。这两种情况在我日常的开发中的使用也是比较多的，今天这篇文章也一样，还是着重分析这两种情况。

#### 1.1 通过构造方法造成的循环依赖
```java
@Service
public class DemoServiceOne {
   DemoServiceTwo demoServiceTwo;
   @Autowired
	 public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	 }
}
```
```java
@Service
public class DemoServiceTwo {
	@Autowired
	public DemoServiceTwo(DemoServiceOne demoServiceOne){
		this.demoServiceOne = demoServiceOne;
	}

	DemoServiceOne demoServiceOne;
}
```
**构造方法循环依赖的结果：**
![构造方法造成循环依赖](https://imgkr.cn-bj.ufileos.com/5748f57e-90f1-40e0-a812-588469b23efd.jpg)
&ensp;&ensp;这里如果对象只有一个构造器，且对象的属性之间存在循环依赖的情况，也会抛出上述的异常。因为：**当对象中有且仅有一个未被注解的构造器时，Spring 实例化对象是通过改构造器来完成的。**

#### 1.2 属性注入造成的循环依赖
```java
@Service
public class DemoServiceOne {
	@Autowired
	DemoServiceTwo demoServiceTwo;

	public void test(){
		System.out.println("hello");
	}
}
```
```java
@Service
public class DemoServiceTwo {

	@Autowired
	DemoServiceOne demoServiceOne;
}
```
**属性注入循环依赖的结果：**
![属性注入对象之间循环依赖](https://imgkr.cn-bj.ufileos.com/bdea3ed8-0c33-4e0a-ab8d-a8b1ea164f6f.jpg)

&ensp;&ensp; `Setter` 方法的注入注入与属性注入类似，这里不在做演示。

&ensp;&ensp; 从上述的两种不同情形的处理结果分析，有结论也有疑问：
* `Spring` 内部帮我们处理了循环依赖，但是他的处理也是有限的，通过构造方法注入的循环依赖是解决不了的。
* 在日常开发中，不要试图去解决循环依赖，而是要在对象注入的时候避免循环依赖，因为一旦出现循环依赖，是很难解决的。
* `Spring` 对于属性注入与`Setter`方法注入的循环依赖是怎么解决的？
* 为什么通过构造器注入的循环依赖就直接抛出异常呢？

&ensp;&ensp;接下来，就到了解惑的时候了，记得谁说的来着：**源码是不会骗人的，你要的答案都在源码里！**

### 3. Spring 解决循环依赖
&ensp;&ensp;在开始分析之前先声明：原型(Prototype)的场景是不支持循环依赖的，通常会走到AbstractBeanFactory类中下面的判断，抛出异常。
![原型bean之间的循环依赖](https://imgkr.cn-bj.ufileos.com/7e632c07-6401-40b5-a1a8-bca0a11c8946.jpg)
&ensp;&ensp;这里我先分析，通过属性注入的情况，通过上一篇文章我们知道，属性注入最终的结果就是 `getBean()`，从而触发新一轮的处理过程。
#### 3.1 构造器注入抛异常
&ensp;&ensp;对于构造器注入的方式，`Spring` 会[找到一个合适的构造器](https://juejin.im/post/5f146ccc5188257109551576)来完成实例化。还记得通过构造方法注入的对象触发依赖注入的地方吗？我们以上述的 ***1.2*** 代码为例，在如下图所示的地方触发依赖注入：
![构造器触发依赖注入](https://imgkr.cn-bj.ufileos.com/b966e685-d21d-4c5c-a73c-7be97ef83326.jpg)
&ensp;&ensp;接下来，看一下这里的方法调用栈：
![构造器依赖注入方法调用栈](https://imgkr.cn-bj.ufileos.com/bdee3a23-b604-44e8-807f-aee8e1d23787.jpg)
&ensp;&ensp;可以看到，最终对于依赖属性的调用还是走到 `getBean()` 方法。好的，这里又要回顾一下上一篇文章了，你是否还记得，属性注入的最终的 `getBean()` 方法的触发入口是在 `populateBean()` 方法中呢？我们先提出掉通过后置处理器处理的部分。其实对于两种依赖的处理到最后都是一样的，通过下述代码示例指出：

* 对于属性注入：在 `org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.AutowiredFieldElement#inject()` 方法中
通过下述代码，来触发依赖注入：
```java
value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
``` 
* 对于构造器注入：在 `org.springframework.beans.factory.support.ConstructorResolver#resolveAutowiredArgument()` 方法中
通过下述代码，来触发依赖注入：
```
return this.beanFactory.resolveDependency(
					new DependencyDescriptor(param, true), beanName, autowiredBeanNames, typeConverter);
```
&ensp;&ensp;也就是说这一部分的处理逻辑是一样的，但是我们依然没有办法判断到底是哪里出问题了。革命尚未成功，同志仍需努力啊...我想了一下，在我刚开始看的时候，这里有很大的疑惑，到底是什么地方出了问题，为什么一个可以，另一个不可以呢？我在这里，使用的方式就是通过断点调试，一点一点的看到底是什么地方抛出的异常，定位问题，然后分析问题。下面就是 `showTime`(有点不要 `face` 了，其实就是苦逼的断点)

* 1.我们要找到异常的场景
   
   &ensp;&ensp;以我上述构造器注入的代码为例，抛出异常的场景是：容器自身触发 `getBean(demoServiceOne)` 通过构造器实例化对象的时候发现需要 `getBean(demoServiceTwo)` 由于 `DemoServiceTwo` 也是通过构造器来完成实例化，在实例化的时候发现需要 `getBean(demoServiceOne)` 这个时候就出现了循环，为了不让 `jvm` 抛出异常，`Spring` 框架做了处理。
* 2. `catch` 异常的地方
![异常分析](https://imgkr.cn-bj.ufileos.com/777a5c3b-bd6a-45da-9814-b732ed3467b8.jpg)
* 3. 抛出异常的地方
![异常来源](https://imgkr.cn-bj.ufileos.com/8fe66b66-f043-4f5b-b1e9-7b04b9055f5d.jpg)
&ensp;&ensp;究其原因，就是在 `DemoserviceTwo` 创建的时候依赖到 `DemoServiceOne`，在去容器中创建 `DemoServiceOne` 的时候，发现 `DemoServiceOne` 正在创建中。

&ensp;&ensp;还记得在 `Spring` 中用来维护正在创建的 `BeanName` 的一个 `Set`集合吗？
```java
private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));
```
&ensp;&ensp;文章分析到这里，好像并没有解决：**为什么注入属性的方式循环依赖是可以的？** 这个问题。不要着急，慢慢来，下面一定会如你所愿的...保证让你满足。。

#### 3.2 属性注入的相安无事
&ensp;&ensp;希望细心的你去仔细看一下，在 `populateBean()` 方法中后置处理器处理的部分，并没有做特殊的处理，来支持属性之间的循环依赖。这里由于篇幅问题，不在做详细的对比。但是可以看到，最终的结果都是： **无论如何，都要通过 `getBean()` 来完成注入对象的获取。** 那么结局问题的关键在哪里？答案就在下面的代码片段中：
```java
boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			/*
			 * setter 方法注入的Bean，通过提前暴露一个单例工厂方法
			 * 从而能够使其他Bean引用到该Bean，注意通过setter方法注入的
			 * Bean 必须是单例的才会到这里来。
			 * 对 Bean 再一次依赖引用，主要应用 SmartInstantiationAwareBeanPostProcessor
			 * 其中 AOP 就是在这里将advice动态织入bean中，若没有则直接返回，不做任何处理
			 *
			 * 在Spring中解决循环依赖的方法：
			 *   在 B 中创建依赖 A 时通过 ObjectFactory 提供的实例化方法来中断 A 中的属性填充，
			 *   使 B 中持有的 A 仅仅是刚初始化并没有填充任何属性的 A，初始化 A 的步骤是在最开始创建A的时候进行的，
			 *   但是 因为 A 与 B 中的 A 所表示的属性地址是一样的，所以在A中创建好的属性填充自然可以通过B中的A获取，
			 *   这样就解决了循环依赖。
			 */
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}
```
&ensp;&ensp;这里是 `doCreateBean()` 方法中的代码片段，我下面将结合这里分析一下，这里为什么可以解决属性注入的循环依赖。

&ensp;&ensp;首先是 `boolean` 变量 `earlySingletonExposure` 的获取：

* 1. `mbd.isSingleton()` 这里与前文原型的`bean`不支持循环依赖呼应
* 2. `this.allowCircularReferences` 在 `Spring` 中默认是允许解决循环依赖的，一般不会有人去设置成否。框架帮我们做难道不香吗？
* 3. `isSingletonCurrentlyInCreation(beanName)` 当第二次获取某个 `bean` 的时候这里返回必然为 `true`。因为在第一次获取的时候，在 `beforeSingletonCreation()` 方法中调用了 `this.singletonsCurrentlyInCreation.add(beanName)` 添加到集合中。
```java
  protected void beforeSingletonCreation(String beanName) {
      if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
        throw new BeanCurrentlyInCreationException(beanName);
      }
    }
```
&ensp;&ensp;综上，所得 `earlySingletonExposure` 为 `true`。

&ensp;&ensp;接下来就是 `addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));`

&ensp;&ensp;首先，`getEarlyBeanReference()` 就是返回被注入的对象，特殊点就是有一些后再处理器，要插手对象的生成。比较特殊的就是有 `Aop` 的要进行动态的增强。
![获取早期bean的引用](https://imgkr.cn-bj.ufileos.com/780ffa7d-ed03-4a74-b14f-a31872bad6ec.jpg)
&ensp;&ensp;接下来就是 `addSingletonFactory()`，我理解的字面意思就是添加生成这个单例的工厂。
```java
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}
```
&ensp;&ensp;上述这简单的几行代码中，涉及到三个 `Map` 的操作。注意重点来了：
**上述三个 `Map` 是 `Spring` 设计的处理属性注入循环依赖的关键**。好，问题来了，你对三个 `Map` 还有印象吗？
```java
/** 用于存放完全初始化好的bean，从该缓存中取出的bean可以直接使用*/
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Cache of singleton factories: bean name to ObjectFactory. */
	/** 存放bean工厂对象，用于解决循环依赖 */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	/** 存放原始的bean对象用于解决循环依赖，存放的对象还未被填充属性 */
	private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
```
&ensp;&ensp;由于 `Spring` 的注释的原因，这三个 `Map` 也就是我们经常听到的 **三级缓存**。下面我将对在 `Spring` 中的使用这三个 `Map` 来解决循环依赖的过程进行分析。首先，通过下面的图来先来看一下在属性注入造成的循环依赖这三个 `Map` 的作用。
![三个Map的使用](https://imgkr2.cn-bj.ufileos.com/11391bf9-849f-4764-8337-440ec3b5e4e8.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=VIy%252BJELe4SUAw5zYbeqATziVOu0%253D&Expires=1596373318)



&ensp;&ensp;在上图中体现了 `A` 依赖 `B`, `B` 依赖 `A`。在 `Spring` 框架中如何来利用三个 `Map` 解决循环依赖的。详细的过程不再分析，如果仔细看这个图的话应该完全可以理解。

&ensp;&ensp;上图中比较难理解的地方在 3.4 之后，这里对这几个步骤做一个简单的解释。3.4 是向容器中 `get` 对象，是因为：
![3.4f分析](https://imgkr2.cn-bj.ufileos.com/9675e90d-e1a7-4434-ae40-80814a6cc093.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=rb%252BdKz4upwIucvWT3QVsCtQSckQ%253D&Expires=1596373784)

然后根据上图中的代码，
```java
this.earlySingletonObjects.put(beanName, singletonObject);
this.singletonFactories.remove(beanName);
```
因此有了图中的 3.5 与 3.6。

&ensp;&ensp;在获取到对象 `A` 之后，`B` 对象中依赖的对象 `A` 返回，才有了下文要叙述的`isTypeMatch()` 方法的调用。

&ensp;&ensp;在对象创建 `B` 对象的依赖注入处理完成之后，根据下面的代码有了上图中的 3.8、3.9、3.9.1

```java
protected void addSingleton(String beanName, Object singletonObject) {
  synchronized (this.singletonObjects) {
    this.singletonObjects.put(beanName, singletonObject);
    this.singletonFactories.remove(beanName);
    this.earlySingletonObjects.remove(beanName);
    this.registeredSingletons.add(beanName);
  }
}
```
&ensp;ensp;上述代码的调用时机就是在对象创建完成之后，入口如下图
![addSingleton入口](https://imgkr2.cn-bj.ufileos.com/21d3de91-409f-4bf9-bdba-2fa3e1644ebf.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=HsmvctsxtzWT%252FTZPSJnzhPgFLec%253D&Expires=1596374332)

&ensp;&ensp;由于 `B` 为 `A` 对象注入的属性。`B` 对象处理完成之后，就是 `A` 对象调用 `isTypeMatch()` 然后在 `addSingleton()` 的过程。


&ensp;&ensp;理解这幅图一定结合**方法调用栈**来理解。最后在说明一下，容器初始化的时候是通过 `for` 循环容器中所有的 `beanName`来处理的`getBean()`。通过上图可以出，在 `A` 和 `B` 有循环依赖的时候，处理完 `A` 的 `getBean()` 在 `singletonObjects` 中已经存放了 `B` 对象。循环到处理 `B` 的
`getBean()` 方法的时候，可以直接从中获取。

&ensp;&ensp;今天这篇文章中的涉及到的知识就是下图中的灰色部分标出的处理流程:
![循环依赖的处理](https://imgkr2.cn-bj.ufileos.com/add7a0c0-21d4-4104-9dc6-212e4aa15984.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=%252BwvUe3lqOjYyD4NK5q7dzNxlLjo%253D&Expires=1596357447)
&ensp;&ensp;这图其实就是整个 `getBean()` 的流程。也是这一段时间文章中写的东西。我会在后面的文章中，对这一个方法在做一次总结，这里贴出来是因为上述 `三个Map的使用` 中只是涉及这三个 `Map` 是怎么用的。但是，没有与整个代码的逻辑结合起来。希望通过这两幅图的结合，能有一个更加清晰的理解。

#### 3.3 关于 earlySingletonObjects
&ensp;&ensp;从上述分析中，我们可以看到 `earlySingletonObjects` 并没有用到。循环依赖的问题主要是通过 `singletonFactories` 来解决的。那么 `Spring` 这样设计的目的在什么地方呢？

&ensp;&ensp;还记得上一篇文章中[Spring 注入对象处理过程](https://juejin.im/post/6854573218277048328)吗？在获取到注入的属性之后，会对被注入的属性与属性的类型做一次匹配。
![注入属性名称与类型匹配](https://imgkr2.cn-bj.ufileos.com/ef4a0c52-7bd5-45ce-936b-35cee1f45553.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=s4HpxQERUYz%252FqVUKpb2N%252BkfCmgk%253D&Expires=1596369680)

&ensp;&ensp;在上述图片中的 `beanFactory.isTypeMatch(autowiredBeanName, field.getType())` 方法中通过 `Object beanInstance = getSingleton(beanName, false);` 在调用一次 `getSingleton()` ，在这个时候 `earlySingletonObjects` 中缓存的元素不为空(**这里用到**)。

&ensp;&ensp;我找了网上有关于 `earlySingletonObjects` 中用来解决被代理的对象循环依赖问题。我个人觉得这里有待商榷。以 `AOP` 为例，在调用 `singletonFactory.getObject()` 通过后置处理器返回的对象就是被代理的对象。

### 总结
&ensp;&ensp;写了这么多，`Spring` 结局循环依赖总结一句话：**在创建对象的时候，提前将生成 *Bean* 的工厂暴露出来，缓存在了singletonFactories中，解决了循环依赖的问题**。

&ensp;&ensp;构造器注入没有办法解决，就是因为对象之间依赖关系的处理，在暴露工厂之前，因此没办法解决。

&ensp;&ensp;循环依赖本身就是问题，只不过是 `Spring` 在框架层面做了最大的努力，来帮助开发人员解决循环依赖。












          












 
