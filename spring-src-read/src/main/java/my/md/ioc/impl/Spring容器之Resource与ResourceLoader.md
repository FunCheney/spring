## Spring容器之Resource 和 ResourceLoader
&ensp;&ensp;这篇文章是填之前文章的坑来了，首先在XmlBeanFactory中，有这么一行代码 `new XmlBeanFactory(new ClassPathResource("spring-bean.xml"));`
其中的 `new ClassPathResource("spring-bean.xml")`没有解释，这就是spring中的**Resource**。

&ensp;&ensp;其次，在 DefaultListableBeanFactory中，`new XmlBeanDefinitionReader(factory)`，中完成了对ResourceLoader的初始化，所谓的ResourceLoader
就是对 Spring 资源加载的统一抽象。

&ensp;&ensp;在这篇文章中，对Spring中的资源，与资源的加载做一个统一学习。

### new ClassPathResource("xxx") 做了什么

#### ClassPathResource 的类关系图


- InputStreamSource 封装任何返货 InputStream 的类，比如File，Classpath下的资源和Byte，Array等。
- Resource 接口抽象了所有Spring内部使用到的底层资源：File，URL，Classpath等。
- ClassPathResource 用来加载classpath 类型资源的实现。使用给定的 ClassLoader 或者给定的 Class 来加载资源。
- ByteArrayResource 对字节数组提供的数据的封装。
- FileSystemResource 文件相关。
- UrlResource url资源的加载。

其中AbstractResource 为 Resource的默认实现。

### new PathMatchingResourcePatternResolver()做了什么

#### PathMatchingResourcePatternResolver 的类关系图


- ResourceLoader 用来加载Spring定义的资源
- DefaultResourceLoader ResourceLoader默认的实现
- ResourcePatternResolver ResourcePatternResolver 是 ResourceLoader 的扩展，它支持根据指定的资源路径匹配模式每次返回多个 Resource 实例。
- PathMatchingResourcePatternResolver 为 ResourcePatternResolver 最常用的子类，它除了支持 ResourceLoader 和 ResourcePatternResolver 新增的 classpath*: 前缀外，还支持 Ant 风格的路径匹配模式（类似于 **/*.xml）。 




