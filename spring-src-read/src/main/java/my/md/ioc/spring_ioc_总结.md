原创：[零星](https://juejin.im/user/465848660669886)（微信公众号ID：Fchen），欢迎分享，转载请保留出处。

>本来我觉的写前面这些东西，有点影响阅读体验。但是今天我着实气愤，所有写的文章被一字不差的拿了过去，一个转载出处也没有。记得谁说过来着做为一个读书人要：“为天地立心，为生民立命，为往圣继绝学，为万世开太平。”。现在，这读书人为了做内容，有点过分了。我很乐于分享与探讨，但是我并不希望我的劳动成果被白嫖。招呼也不打，转载出处也没有？搜云库技术团队，你关注我了，如果你看到这篇文章，请在你们的自建博客(我掘金上面的文章都被原封不动的复制过去了)上注明出处。

## 跳出来看全貌
&ensp;&ensp;首先 `IoC` 是一个概念：**控制反转(依赖反转)**；用来为：**对象间依赖关系的处理** 提供指导思想。

&ensp;&ensp;其次 `Spring IoC` 容器，是用来实现这个指导思想的方式，来解决对象之间的耦合关系。可以说它促进的 `IoC` 设计模式的发展，同时也促成了产品化 `IoC`容器的出现。

&ensp;&ensp;然后 `IoC` 统一集中处理对象之间的依赖关系。`Spring` 实现 `IOC` 的思路是提供一些配置信息用来描述类之间的依赖关系，然后由容器去解析这些配置信息，
继而维护对象之间的依赖关系。但是，前提是对象之间的依赖关系必须在类中定义好。

&ensp;&ensp;最后 `Spring` `IoC` 提供了一个基本的 `javaBean`容器，通过`IoC`
模式管理依赖关系，并通过依赖注入和 `AOP` 切面增强了为 `JavaBean` 这样的 `POJO` 对象赋予事务管理、生命
周期管理等基本功能。

&ensp;&ensp;在应用的开发中，在开发设计组件时，往往需要引用和调用其他组件的服务，这种依赖关系
如果固化在组件设计中，就会造成依赖关系的复杂，维护成本增加。这个时候，如果使用 `IoC` 容器，把资源获取
的方向反转，让 `IoC` 容器主动管理这些依赖关系，将这些依赖关系注入到组件当中，那么会让这些依赖关系的适配
和管理更加灵活。



## 钻进去看细节
&ensp;&ensp;这里要展示，`Spring IoC` 设计的细节，首先通过类图简单的有一个认识。


### 1 Spring IoC 容器的设计与实现
![容器设计路径](https://imgkr2.cn-bj.ufileos.com/f66625b6-7035-40ba-96f2-f48e0320bfd5.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=SpIJZq0MoSVMKSsrB84B4oDi%252Bos%253D&Expires=1596932838)
&ensp;&ensp;点击了解：[Spring Ioc](https://juejin.im/post/6844904147032997901)。这里做一个简单的总结，`Spring IoC` 是 `IoC` 的产品。就好比说，`IoC` 是一个水池，里面的水可以比作我们想要的对象。我们需要水的时候，就去水桶中拿。

&ensp;&ensp;这里简单总结面试题：**BeanFactory与ApplicationContext的区别**以及**BeanFactory 与 FactoryBean的区别**。

①：`BeanFactory` 是 `Spring` 中实现 `IoC` 来提供自己的 `IoC` 容器的顶级接口。也就是说，这是水桶最基础的模型。而 `ApplicationContext` 则就是那个贴了砖，具有加热功能？等等... 添加了一些列功能的水池。

②：`BeanFactory` 既然是池子，那就是说他的主要功能是装水，或者说给使用者提供可以使用的水。而 `FactoryBean`则是在池子里放的水的另一种类型。具体详细的区别这就不在做赘述了，之前的文章已经分析过了。

### 2 创建 Spring Ioc 的准备
&ensp;&ensp;我这一系列的文章都是分析 `AnnotationConfigApplicationContext` 的，这里的总结也不例外，主要有两个原因，①：我在日常的开发中使用的是基于注解和 `javaConfig` 形式的；②：`Spring` 本身也在去 `xml` 化。当然对于 `xml` 形式的，只能以后如果有机会在研究了。

&ensp;&ensp;容器的初始化通过 `ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);` 开始，主要分为以下几个过程：

①：通过 `new` 来完成对象的实例化，通过构造函数来完成类中属性的实例化：读取器（`reader`）将 `spring` 中加了注解的类转化为 一个 `spring` 当中 `bean` 的描述文件；实例化扫描器（`scanner`）能够扫描一个类，并转换为`spring` 当中 `bean` 的描述文件。注意这里提供出来的这个扫描器要显示调用才生效。

![容器雏形](https://imgkr2.cn-bj.ufileos.com/f4f11a18-3f7f-46c2-8b35-2bee17fa6a59.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=TmgJPL1F7CS%252F6ibYa0vkWhzd9Ls%253D&Expires=1596968734)

②：`new` 方法调用完成之后，就开始向容器中注册 `Bean`；这里需要注意的是，在这个阶段 `Spring` 会将我们定义好的 `Bean` 进行封装，转化成对应的`BeanDefinition` 并设置一些属性(如果需要)，如：`scope`、`lazyInit`、`primary` 等。并在改过程中，将其添加到 `BeanDefinitionMap` 中去。

![容器初具规模](https://imgkr2.cn-bj.ufileos.com/33416f36-e1cb-4881-83d3-5c38fed30150.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=732gAtEtLH4mMWt1DeM9v%252FJ7SH4%253D&Expires=1596970074)

这里需要注意以下几点：
 
 * 上述 ① 程中，`Spring` 中[内置的对象](https://juejin.im/post/6844904167002095630)已经注册到 `BeanDefinitionMap`
 * 上述 ② 过程中，向容器注入的是我们定义的配置类
 * 通过包扫描的方式添加的 `Bean` 并不是在这里注册到 `BeanDefinitionMap` 中去。

&ensp;&ensp;至此容器中比较基础的东西都已添加到里面了，但是还有一些非常重要的过程，那就是 **初始化spring的环境。*

### 3 Spring IoC 的形成

&ensp;&ensp;最最最重要的方法 `refresh()` 方法了，这个方法如果到现在还不知道的，可以拉出枪毙五分钟了... 如果你不想了解这个方法，那么我劝你放弃 `Spring`。

![refresh方法](https://imgkr2.cn-bj.ufileos.com/55b1e528-75dc-4779-8957-8afba1eff465.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=um2%252FEWM3S6%252FbnQmiePjHtP%252By59w%253D&Expires=1596966135)

&ensp;&ensp;在上述流程图中我找到了两个最最最重要的方法：

①：`invokeBeanFactoryPostProcessors()` 这里会执行 `Bean` 工厂的后置处理器，大名鼎鼎的 `doScan()` 方法就是在这里调用的，经过这里之后我们自己定义的 `Bean` 也被注册到 `BeanDefinitionMap` 中去了。

![我们定义的对象在容器中了](https://imgkr2.cn-bj.ufileos.com/cb5213e4-c6e9-43f4-a220-7e5d1e9acfb2.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=syS2mm9q1XMaelAz%252B5BKj2pm4Vs%253D&Expires=1596971144)


②：`finishBeanFactoryInitialization()` 这里会执行 `getBean()` 方法，容器中获取 `Bean`。完成对象实例化、依赖关系的建立等。

![实例化Bean的过程](https://imgkr2.cn-bj.ufileos.com/8b1e6ceb-98ff-4b8e-914e-fc1f6d010130.jpg?UCloudPublicKey=TOKEN_8d8b72be-579a-4e83-bfd0-5f6ce1546f13&Signature=UwRfkm1b2r5Sr3Qnfxc5wxeGTnU%253D&Expires=1596971404)

&ensp;&ensp;至此，`Spring` 环境就已经基本上创建了，当然还有一些部分如：初始化上下文中的事件机制，发布容器事件，结束 `refresh` 过程，以及异常的处理等，这里并没有做总结。

### 最后
1. `IoC` 系列的文章还没有写完，但是由于要做一个组内分享，只能先做一个总结，回顾一下之前的知识。后面 `IoC` 的文章应该还会继续写，毕竟有始有终吗？
2. "看那个人好像一条狗啊！" 仅用这句话，表达写文章的不易，希望有些人能注意一下。
3. 文章如有不对之处，还是请指出。还是那句话，我乐于分享与探讨。
4. 这里只是做总结，并没有很细节描述每一个部分，如有需要，还请查看之前的文章。
























