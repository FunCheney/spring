### Spring容器之Resource 和 ResourceLoader
&ensp;&ensp;这篇文章是填之前文章的坑来了，首先在XmlBeanFactory中，有这么一行代码 `new XmlBeanFactory(new ClassPathResource("spring-bean.xml"));`
其中的 `new ClassPathResource("spring-bean.xml")`没有解释，这就是spring中的**Resource**。

&ensp;&ensp;其次，在 DefaultListableBeanFactory中，`new XmlBeanDefinitionReader(factory)`，中完成了对ResourceLoader的初始化，所谓的ResourceLoader
就是对 Spring 资源加载的统一抽象。

&ensp;&ensp;在这篇文章中，对Spring中的资源，与资源的加载做一个统一学习。

