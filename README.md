# Spring Framework 源码阅读

## 一、环境搭建与代码编译
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
## IOC概述
&ensp;&ensp;[IOC概述]

## 二、spring IOC

### spring中IOC容器的系列设计与实现
&ensp;&ensp;[spring中IOC]

### 1、BeanFactory
#### 1.1 使用
&ensp;&ensp;[BeanFactory详解]
##### XmlBeanFactory
&ensp;&ensp;[XmlBeanFactory使用与详解]
##### 编程式使用IOC容器
&ensp;&ensp;[DefaultListableBeanFactory使用]

### 2、ApplicationContext

#### 2.1 使用
#### ClassPathXmlApplicationContext
&ensp;&ensp;[ClassPathXmlApplicationContext使用]
#### FileSystemXmlApplicationContext
&ensp;&ensp;[FileSystemXmlApplicationContext使用]

#### AnnotationConfigApplicationContext
从下面这段代码开始：
``` java
AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(MyConfig.class);
```

### 3、Spring中的一些概念

#### Bean的描述 BeanDefinition
&ensp;&ensp;[Spring中Bean的描述BeanDefinition]

#### Resource(资源) 和 ResourceLoader(资源解析)

&ensp;&ensp;[Spring容器之Resource 和 ResourceLoader]

### 4、IoC容器的初始化

#### 1、BeanDefinition的定位

##### 注解方式定义的BeanDefinition的定位

##### xml方式定义的BeanDefinition的定位

#### 2、BeanDefinition的加载

#### 3、BeanDefinition的注册


## 三、spring AOP
### AOP

#### 实现方式

##### 静态代理

- 基于继承的方式实现

- 基于聚合的方式实现

##### 动态代理 

- 基于JDK的动态代理

- 基于CGLIB实现的动态代理

file --> class --> byte[] --> object(class)

## 四、Spring 日志
### java日志体系
#### log4j
#### slf4j
#### logback
#### jcl

### spring 重写 JCl


[IOC概述]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/IOC%20%E6%A6%82%E8%BF%B0.md
[spring中IOC]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/Spring%E4%B9%8BIOC.md
[BeanFactory详解]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BBeanFactory.md
[XmlBeanFactory使用与详解]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BXmlBeanFactory.md
[DefaultListableBeanFactory使用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BDefaultListableBeanFactory.md
[ClassPathXmlApplicationContext使用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BClassPathXmlApplicationContext.md
[FileSystemXmlApplicationContext使用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BFileSystemXmlApplicationContext.md

[Spring容器之Resource 和 ResourceLoader]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BResource%E4%B8%8EResourceLoader.md

[Spring中Bean的描述BeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/Spring%E4%B9%8B%E5%AF%B9%E8%B1%A1%E6%8F%8F%E8%BF%B0BeanDefinition.md