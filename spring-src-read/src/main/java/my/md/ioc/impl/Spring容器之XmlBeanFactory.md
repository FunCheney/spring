### 从一个过期的Spring IoC 容器说起
&ensp;&ensp;XmlBeanFactory是一个Spring IoC容器的实现，可以看到，这个类已经被弃用了，但是因为这个类比较简单。
因此，可以通过这个类来学习简单IoC容器的设计原理。
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/xmlBeanfactory.png">
 </div>

### XmlBeanFactory 的使用与源码解析
&ensp;&ensp;以XmlBeanFactory为例来说明简单IoC容器的实现原理。

#### 使用
```

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
	 * @param resource the XML resource to load bean definitions from
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * Create a new XmlBeanFactory with the given input stream,
	 * which must be parsable using DOM.
	 * @param resource the XML resource to load bean definitions from
	 * @param parentBeanFactory parent bean factory
	 * @throws BeansException in case of loading or parsing errors
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
		/** 调用父类的构造方法*/
		super(parentBeanFactory);
		/** 使用 XmlBeanDefinitionReader，调用loadBeanDefinitions*/
		this.reader.loadBeanDefinitions(resource);
	}

}
```