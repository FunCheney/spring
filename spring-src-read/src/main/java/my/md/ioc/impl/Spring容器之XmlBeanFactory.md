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
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/xmlBeanfactory-XmlBeanDefinitionReader.png">
 </div>

* 1.通过继承AbstractBeanDefinitionReader 中的方法，来使用 ResourceLoader 将资源文件路径转换
为对应的Resource 文件。

* 2.通过DocumentLoader 对 Resource 文件进行转换，将Resource文件转换为 Document 文件。

* 3.通过实现BeanDefinitionDocumentReader 接口的 DefaultBeanDefinitionDocumentReader类，
对Document进行解析，并使用BeanDefinitionParserDelegate对Element进行解析。

#### XmlBeanFactory 的初始化
&ensp;&ensp;从这一行代码开始
```java
BeanFactory bf = new XmlBeanFactory(new ClassPathResource("spring-bean.xml"));
```
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/XmlBeanFactory_init.png">
 </div>

&ensp;&ensp;构造XmlBeanFactory这个IoC容器时，需要指定的BeanDefinition的信息来源，而这个信息
来源需要封装成Spring的Resource类来给出。Resource是Spring用来封装I/O操作的类。然后将Resource作为
构造参数传递给XmlBeanFactory构造函数。这样，IoC容器就可以方便定位到需要的BeanDefinition信息来对Bean
完成容器的初始化和依赖注入过程。

&ensp;&ensp;对XMLBeanDefinitionReader对象的初始化，以及使用这个对象来完成loadBeanDefinitions的
调用，就是这个调用启动从Resource中加载BeanDefinition的过程，loadBeanDefinitions()同时也是IoC容器
初始化的重要组成部分。

**注: 这里请记住 loadBeanDefinitions() 这个方法，在以后的有关IoC的其他实现类中，Spring不之一次的用到这个方法。**


#### XmlBeanFactory中的两行代码
&ensp;&ensp;对于XmlBeaFactory的调用最后都是通过`super(parentBeanFactory);` 与 `this.reader.loadBeanDefinitions(resource);`来完成上述的功能。
下面，就来看一下这两行代码都做了些什么事情。

##### super(parentBeanFactory)
&ensp;&ensp;代码的调用顺序，结合XmlBeanFactory的类关系的继承图，可以看到其调用顺序如下：
①: XmlBeanFactory#super(parentBeanFactory) 其中parentBeanFactory 为null；

②: DefaultListableBeanFactory#super(null)

③: AbstractAutowireCapableBeanFactory#this()

④: AbstractAutowireCapableBeanFactory#super() 这里会调用 ignoreDependencyInterface()相关的方法

⑤: AbstractBeanFactory() 调用AbstractBeanFactory 的无惨构造方法

&ensp;&ensp;这里创建的XmlBeanFactory在继承了DefaultListableBeanFactory容器的功能的同时，增加了新的功能。


##### this.reader.loadBeanDefinitions(resource)
&ensp;&ensp;下面重点看一下loadBeanDefinitions()的调用顺序。

①: XmlBeanDefinitionReader#loadBeanDefinitions(Resource resource)

②: XmlBeanDefinitionReader#loadBeanDefinitions(new EncodedResource(resource))

    a: EncodedResource()#this(resource, null, null)
    
    b: EncodedResource(Resource resource, @Nullable String encoding, @Nullable Charset charset)
    
    c: Object()

③: XmlBeanDefinitionReader#loadBeanDefinitions(EncodedResource encodedResource)    

    a: encodedResource.getResource().getInputStream()
    
④: XmlBeanDefinitionReader#doLoadBeanDefinitions(InputSource inputSource, Resource resource)

⑤: XmlBeanDefinitionReader#registerBeanDefinitions(Document doc, Resource resource)

⑥: XmlBeanDefinitionReader#createBeanDefinitionDocumentReader()

⑦: DefaultBeanDefinitionDocumentReader#registerBeanDefinitions(Document doc, XmlReaderContext readerContext)

⑧: DefaultBeanDefinitionDocumentReader#doRegisterBeanDefinitions(Element root)

⑨: DefaultBeanDefinitionDocumentReader#parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate)

⑩: BeanDefinitionParserDelegate#parseCustomElement(org.w3c.dom.Element)

11: DefaultBeanDefinitionDocumentReader#(Element ele, BeanDefinitionParserDelegate delegate)

12: BeanDefinitionReaderUtils#registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)

13: DefaultListableBeanFactory#registerBeanDefinition(String beanName, BeanDefinition beanDefinition)

14: DefaultListableBeanFactory#beanDefinitionMap.put(beanName, beanDefinition);