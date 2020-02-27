### 编程式使用IOC容器
``` java
public class IocTest {
	public static void main(String[] args) {
		ClassPathResource resource = new ClassPathResource("spring-bean.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);
	}
}
```
使用IOC 容器时的几个步骤

- 1.创建IOC配置文件的抽象资源，其中包含了BeanDefinition的定义
- 2.创建一个BeanFactory，这里使用的是DefaultListableBeanFactory
- 3.创建一个载入BeanDefinition的读取器，这里使用XmlBeanDefinitionReader来载入xml文件
形式的BeanDefinition，通过一个回调配回给BeanFactory
- 4.从定义好的资源位置读入配置信息，具体的解析过程由XmlBeanDefinitionReader来完成。

完成整个bean的载入和注册之后，需要的IOC容器就建立起来了，可供程序员使用。

### new DefaultListableBeanFactory() 干了什么

** DefaultListableBeanFactory 的类关系**

&ensp;&ensp;首先看一下DefaultListableBeanFactory的类关系的继承图：

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/DefaultListableFactory_class_relation.jpg
">
 </div> 
 
 **new DefaultListableBeanFactory() 调用关系**
 
&ensp;&ensp;①. DefaultListableBeanFactory()#super()

&ensp;&ensp;②. AbstractAutowireCapableBeanFactory()#super();

&ensp;&ensp;③. AbstractBeanFactory()

 **new XmlBeanDefinitionReader(factory)** 
 
 &ensp;&ensp;XmlBeanDefinitionReader相关的类在XmlBeanFactory中介绍过了，这里不做详细介绍。
 主要看看，new XMLBeanDefinitionReader()的调用过程。
 
 &ensp;&ensp;①. XmlBeanDefinitionReader#super(registry)
 
 &ensp;&ensp;②. 实例化 ResourceLoader
 
 &esnp;&ensp;ResourceLoader 为 Spring 资源加载的统一抽象，具体的资源加载则由相应的实现类来完成.

**reader.loadBeanDefinitions(cpr)**

&ensp;&ensp;这里的`reader.loadBeanDefinitions()`与之前的XmlBeanFactory中的 `loadBeanDefinitions(resource)`
的加载过程一致。