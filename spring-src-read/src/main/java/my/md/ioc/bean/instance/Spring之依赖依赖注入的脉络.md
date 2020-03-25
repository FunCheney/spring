### Spring IoC容器依赖注入的脉络
&ensp;&ensp;Spring的IoC容器的依赖注入是一个复杂的过程，首先对其依赖注入的整体流程有一个认识，然后在对其
细节逐个击破。

&ensp;&ensp;依赖注入一般情况下是发生在用户第一项容器获取 Bean的过程中，这个过程是发生在容器中的BeanDefinition
数据已经建立好的前提下进行的。`getBean()` 是依赖注入的起点，下面就先看一下依赖注入的大致过程。

#### `getBean()`的大致过程

##### 1.时序图
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnntionConfigApplicationContext_class_relation.jpg">
 </div>


