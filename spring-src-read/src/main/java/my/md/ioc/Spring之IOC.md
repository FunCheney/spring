### Spring 之 IOC

#### Spring 实现IoC 的思路和方法

&ensp;IoC 统一集中处理对象之间的依赖关系。`Spring`实现IOC的思路是提供一些配置信息用来描述类之间的依赖关系，然后有容器去解析这些配置信息，继而维护对象之间的依赖关系，前提是对象之间的依赖关系必须在类中定义好。

方法：

1. 应用程序中提供类，提供依赖关系(属性或者构造方法)
2. 把需要交给容器管理的对象通过配置信息告诉容器(xml，annotation、javaconfig)
3. 把各个类之间的依赖关系通过配置信息告诉容器


&ensp;&ensp;IOC可以理解为一种设计思想、设计模式用来解耦组件之间的复杂关系。在Spring 中，
Spring IOC 就是对这种设计模式的实现，Spring IoC提供了一个基本的javaBean容器，通过IoC
模式管理依赖关系，并通过依赖注入和AOP切面增强了为JavaBean这样的POJO对象赋予事务管理、生命
周期管理等基本功能。

&ensp;&ensp;在应用的开发中，在开发设计组件时，往往需要引用和调用其他组件的服务，这种依赖关系
如果固化在组件设计中，就会造成依赖关系的复杂，维护成本增加。这个时候，如果使用IoC容器，把资源获取
的方向反转，让IoC容器主动管理这些依赖关系，将这些依赖关系注入到组件当中，那么会让这些依赖关系的适配
和管理更加灵活。

* 接口注入

* setter注入
&ensp;&ensp;setter注入也称设值注入，IoC容器通过成员变量的setter方法来注入被依赖的对象。这种注入方式
简单、直观，因而在Spring的依赖注入中大量使用。

* 构造器注入
&ensp;&ensp;通过构造器来完成成员变量的注入，就是驱动Spring在底层以反射的方式执行带指定参数的构造器，当执行带
参数的构造器时，就可以利用构造器参数对成员变量执行初始化。

#### Spring IoC 容器的设计与实现

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/bean/BeanDefinition_class.jpg">
 </div>
 
 &ensp;&ensp;上述图片中标注出两条BeanFactory接口的设计路径，一条是蓝色线条表示，一条红色线条表示。
 
* 从接口 `BeanFactory` 到 `HierarchicalBeanFactory`，再到 `ConfigurableBeanFactory`，是一条 `BeanFactory`
  设计路径。在 `BeanFactory` 中定义了最基本的 IoC 容器的功能，包含有 `getBean()` IoC 中最基础的方法;
  在 `HierarchicalBeanFactory` 中继承了 `BeanFactory` 接口，并增加了 `getParentBeanFactory()`的接口功能，
  使 `BeanFactory` 具备了双亲 IoC 容器的管理功能；在 `ConfigurableBeanFactory` 接口中，定义了一些 `BeanFactory`
  的配置功能，比如 `setParentBeanFactory()` 设置双亲IoC容器，通过 `addBeanPostProcessor()` 配置 Bean 后置处理
  器，等等。
  
* 从 `BeanFactory` 到 `ListableBeanFactory`，再到 `ApplicationContext`。这条路径是另一条主要的设路径。`ListableBeanFactory` 和 `HierarchicalBeanFactory`
两个接口连接 `BeanFactory` 接口定义和 `ApplicationContext` 应用上线文的接口定义。在 `ListableBeanFactory` 中
细化了许多 `BeanFactory` 的功能，如 `getBeanDefinitionNames()` 接口方法；`HierarchicalBeanFactory` 如上所述。
 


### IoC容器的初始化过程
&ensp;&ensp;IoC容器的启动包括以下的三个过程，具体来说，这个启动包括BeanDefinition的Resource定位、
载入和注册三个过程。

&ensp;&ensp;第一个过程是Resource定位过程。这个Resource定位指的是BeanDefinition的资源定位，它由
ResourceLoader通过统一的Resource接口来完成，这个Resource对各种形式BeanDefinition的使用都提供统
一的接口。

&ensp;&ensp;第二个过程是BeanDefinition的载入。这个载入过程是把用户定义好的Bean表示成IoC容器内部的
数据结构，而这个容器内部的数据结构就是BeanDefinition。

&ensp;&ensp;第三个过程是向IoC容器注册这些BeanDefinition的过程。这个过程调用BeanDefinitionRegistry
接口的实现来完成。这个注册过程把载入过程中的解析得到的BeanDefinition向IoC容器进行注册。就是在IoC容器内部
将BeanDefinition注入到一个HashMap中去，IoC容器就是通过这个HashMap来保存BeanDefinition的数据。

&ensp;&ensp;后续在介绍IoC容器的实现时，会根据这三个阶段来分别说明IoC容器中BeanDefinition的定位、加载、
注册。