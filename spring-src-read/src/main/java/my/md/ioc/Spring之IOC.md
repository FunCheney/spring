### Spring 之 IOC
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

* 构造器注入


#### Spring IoC 容器的设计与实现




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