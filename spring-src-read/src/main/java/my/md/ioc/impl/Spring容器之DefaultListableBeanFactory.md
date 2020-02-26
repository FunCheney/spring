## 编程式使用IOC容器
``` java
public class IocTest {
	public static void main(String[] args) {
		ClassPathResource cpr = new ClassPathResource("spring-bean.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(cpr);
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

#### DefaultListableBeanFactory 的类关系
 