# Spring Framework 源码阅读
## 环境搭建与代码编译
- 1.在github 上克隆代码。
- 2.打开 import-into-idea.md 按照该文件中步骤 操作。
----
- 1.新建一个module 命名 spring-study
- 2.在其中测试spring 环境是否搭建完毕
    
&ensp;&ensp;注意在spring-study module下的build.gradle 中添加 `compile(project(":spring-context"))`。

### 环境测试
工程目录如下：

#### 新建配置类
```java
@Configuration
@ComponentScan("com.fchen")
public class MyConfig {
}
```
#### 新建逻辑测试类
```java
@Service
public class MyService {

	public void test(){
		System.out.println("hello test");
	}
}
```
#### 新建Spring启动测试类
```java
public class MyTestStart {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		MyService myService = ann.getBean(MyService.class);
		myService.test();
	}
}
```
## Spring 几个重要概念
#### 控制翻转 与 依赖注入


#### Spring中Bean的描述BeanDefinition

Spring通过定义BeanDefinition来管理基于Spring的应用中的各种对象以及他们之间的相互依赖关系。
在Spring中通过BeanDefinition来描述一个Bean，通过这个类来设置Spring中Bean的属性，加载方式。
BeanDefinition抽象了对Bean的定义，是让容器起作用的主要数据类型

#### Spring中对BeanDefinition的封装BeanDefinitionHolder


## spring IOC
### spring中IOC容器的系列设计与实现


#### BeanFactory

#### XmlBeanFactory

编程式使用IOC容器
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


#### ApplicationContext
### IOC 容器的初始化过程
从下面这段代码开始：
``` java
AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(MyConfig.class);
```

##### ClassPathXmlApplicationContext
##### FileSystemXmlApplicationContext 

## spring AOP
### AOP

#### 实现方式

##### 静态代理

- 基于继承的方式实现

- 基于聚合的方式实现

##### 动态代理 

- 基于JDK的动态代理

- 基于CGLIB实现的动态代理

file --> class --> byte[] --> object(class)
