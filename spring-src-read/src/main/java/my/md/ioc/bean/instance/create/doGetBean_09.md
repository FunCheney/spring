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

![对象中的属性时自己](https://imgkr.cn-bj.ufileos.com/1c90d24b-770e-4051-9fba-f86e38c927eb.jpg)

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

&ensp;&ensp;在上一篇文章中，我们分析了 `@Autowiued` 注解构造器以及注解属性的方式。这两种情况在我日常的开发中的使用也是比较多的，今天这篇文章也一样，还是着重分析这两中情况。

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
&ensp;&ensp;这里如果对象只有一个构造器，且对象的属性之间存在循环依赖的情况，也会抛出上述的异常。因为：**当对象中有且仅有一个未被注解的构造器时，Spring 实例话对象是通过改构造器来完成的。**

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





 
