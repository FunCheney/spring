### Spring IoC容器依赖注入的脉络
&ensp;&ensp;Spring的IoC容器的依赖注入是一个复杂的过程，首先对其依赖注入的整体流程有一个认识，然后在对其
细节逐个击破。

&ensp;&ensp;依赖注入一般情况下是发生在用户第一项容器获取 Bean的过程中，这个过程是发生在容器中的BeanDefinition
数据已经建立好的前提下进行的。`getBean()` 是依赖注入的起点，下面就先看一下依赖注入的大致过程。

#### `getBean()`的大致过程

##### 1.时序图
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E7%9A%84%E8%84%89%E7%BB%9C%E5%9B%BE.jpg">
 </div>

 &ensp;&ensp;这里大概介绍一下上述流程中每个方法大致的功能，至于每个里面的代码细节，采取逐个击破的策略，一个一个的来
 拿下。。

①：`getBean()`方法是依赖注入的起点，都是通过用户向容器中索取 Bean 的时候封装的方法，  
这个方法定义在IoC容器的顶级接口 `BeanFactory`中。这里是对该方法的重写。

②：`doGetBean()`方法是真正获取Bean的方法，通过学习Spring的源码，不难发现，这是Spring的一种分割，在
Spring中真正做事情的方式都是以 `doXXXX`来实现的。

③：`createBean()`方法，创建singleton bean的实例。

④：`doCreateBean()`方法，是真正创建Bean的方法。

⑤：`createBeanInstance()`方法，生成 `BeanWrapper` 对象，来完成Bean所包含的java对象的创建。

⑥：`instantiateBean()`方法，通过默认的无参构造方法进行实例化。

⑦：`instantiate()`方法，根据不同的实例化策略对Bean进行实例。

⑧：`populateBean()`方法，Bean的依赖关系处理过程。

⑨：`applyPropertyValues()`方法，完成属性的注入。

⑩：`resolveValueIfNecessary()`方法，对bean的Reference解析。










