### 从一个过期的Spring IoC 容器说起
&ensp;&ensp;XmlBeanFactory是一个Spring IoC容器的实现，可以看到，这个类已经被弃用了，但是因为这个类比较简单。
因此，可以通过这个类来学习简单IoC容器的设计原理。
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/xmlBeanfactory.png">
 </div>

&ensp;&ensp;从类的继承关系图中，可以看出 XmlBeanFactory 是以 DefaultListableBeanFactory为父类，
来实现自己特有的功能。这里请记住 **DefaultListableBeanFactory** 这个类是一个非常重要的类，在Spring的
IoC容器中有着极其重要的地位， DefaultListableBeanFactory 包含了基本IoC容器所具有的重要功能。在Spring
中将其作为一个默认的功能完整的IoC容器拉来使用。

### XmlBeanFactory 的使用与源码解析
&ensp;&ensp;以XmlBeanFactory为例来说明简单IoC容器的实现原理。

#### 使用
```java
public class XmlBeanFactoryTest {
	public static void main(String[] args) {
		BeanFactory bf = new XmlBeanFactory(new ClassPathResource("spring-bean.xml"));
		XmlBeanFactoryService xmlServiceTest = (XmlBeanFactoryService)bf.getBean("xmlServiceTest");
		xmlServiceTest.say();
	}
}
```
```java
public class XmlBeanFactoryService {

	public void say(){
		System.out.println("hello");
	}
}
```

#### 源码
```java
public class XmlBeanFactory extends DefaultListableBeanFactory {

	/**
	 * 初始化 XmlBeanDefinitionReader 对象
	 * 处理以 xml 方式定义的 BeanDefinition
	 */
	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);


	/**
	 * Create a new XmlBeanFactory with the given resource,
	 * which must be parsable using DOM.
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * Create a new XmlBeanFactory with the given input stream,
	 * which must be parsable using DOM.
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
		/** 调用父类的构造方法*/
		super(parentBeanFactory);
		/** 使用 XmlBeanDefinitionReader，调用loadBeanDefinitions*/
		this.reader.loadBeanDefinitions(resource);
	}

}
```

&ensp;&esnp;构造XmlBeanFactory时，需要指定BeanDefinition的信息来源，这个信息来源需要封装成
Spring中的Resource类。在XmlBeanFactory中定义了一个XmlBeanDefinitionReader对象，并对其进行
了初始化，有了这个对象，通过Xml方式定义的BeanDefinition就有了处理的地方，XmlBeanDefinitionReader
主要完成对Xml 形式的信息处理。

**XmlBeanDefinitionReader对象**

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/xmlBeanfactory-XMLBeanDefinitionReader.png">
 </div>

* 1.通过继承AbstractBeanDefinitionReader 中的方法，来使用 ResourceLoader 将资源文件路径转换
为对应的Resource 文件。

* 2.通过DocumentLoader 对 Resource 文件进行转换，将Resource文件转换为 Document 文件。

* 3.通过实现BeanDefinitionDocumentReader 接口的 DefaultBeanDefinitionDocumentReader类，
对Document进行解析，并使用BeanDefinitionParserDelegate对Element进行解析。
