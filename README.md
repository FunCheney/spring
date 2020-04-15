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

* [AnnotationConfigApplicationContext使用]

* [AnnotationConfigApplicationContext源码解析一]

* [AnnotationConfigApplicationContext源码解析二]

* [AnnotationConfigApplicationContext源码解析三]

* [AnnotationConfigApplicationContext源码解析四]

* [AnnotationConfigApplicationContext源码解析五]

* [AnnotationConfigApplicationContext源码解析六]


### 3、Spring中的一些概念

#### Bean的描述 BeanDefinition
&ensp;&ensp;[Spring中Bean的描述BeanDefinition]

##### BeanDefinition 的子类
* [AbstractBeanDefinition]

* [RootBeanDefinition]

* [ChildBeanDefinition]

* [GenericBeanDefinition]

* [AnnotatedGenericBeanDefinition]

* [ScannedGenericBeanDefinition]

##### BeanDefinition的封装
&ensp;&ensp;[BeanDefinitionHolder]

#### Resource(资源) 和 ResourceLoader(资源解析)

&ensp;&ensp;[Spring容器之Resource 和 ResourceLoader]

### 4、IoC容器的初始化

#### 1、BeanDefinition的定位

##### 注解方式定义的BeanDefinition的定位

##### xml方式定义的BeanDefinition的定位

#### 2、BeanDefinition的加载

#### 3、BeanDefinition的注册

#### 4、spring中的依赖注入

&ensp;&ensp;&ensp;[spring中的依赖注入脉络图]

&ensp;**1.** &ensp;[Spring依赖注入之getBean()]

   *  [getBean()之doGetBean源码阅读一]

   *  [getBean()之doGetBean源码阅读二]

&ensp;**2.** &ensp;Spring依赖注入之createBean()

   *  [`singleton` Bean的实例化过程]

   *  [`prototype` Bean的实例化过程]

   *  [`scope` Bean的实例化过程]

   *  [准备创建 bean]
       -  [doCreateBean预览]

       -  [createBeanInstance实现]

          +  [使用工厂方法对Bean进行实例化]

          +  [通过构造器的方式注入对象]

          +  [默认的实例化 `instantiateBean()`]

       -  [MergedBeanDefinitionPostProcessor 的应用]

          + `@Autowired` 处理
          + `@PostConstruct` & `PreDestroy` & `@Resource` 处理

       -  [Bean的依赖关系处理]

           +  [自动装配的实现]

           +  [applyPropertyValues]

       -  [将原生对象变成代理对象]

## 三、spring AOP
### AOP

#### 代理模式
* 静态代理

    - 基于继承的方式实现

    - 基于聚合的方式实现

* 动态代理

   - 基于JDK的动态代理

   - 基于CGLIB实现的动态代理

file --> class --> byte[] --> object(class)

#### Spring Aop 源码

* [开头]
* [创建Aop代理]
  - [获取增强器]
  - 寻找匹配的增强器
  - 创建代理


## 四、Spring 日志
### java日志体系
#### log4j
#### slf4j
#### logback
#### jcl

### spring 重写 JCl

## 五、Spring webMvc


## 六、Spring 事件监听机制


[IOC概述]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/IOC%20%E6%A6%82%E8%BF%B0.md
[spring中IOC]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/Spring%E4%B9%8BIOC.md
[BeanFactory详解]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BBeanFactory.md
[XmlBeanFactory使用与详解]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BXmlBeanFactory.md
[DefaultListableBeanFactory使用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BDefaultListableBeanFactory.md
[ClassPathXmlApplicationContext使用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BClassPathXmlApplicationContext.md
[FileSystemXmlApplicationContext使用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BFileSystemXmlApplicationContext.md
[AnnotationConfigApplicationContext使用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BAnnotationConfigApplicationContext_1.md
[AnnotationConfigApplicationContext源码解析一]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BAnnotationConfigApplicationContext_2.md
[AnnotationConfigApplicationContext源码解析二]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BAnnotationConfigApplicationContext_3.md
[AnnotationConfigApplicationContext源码解析三]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BAnnotationConfigApplicationContext_4.md
[AnnotationConfigApplicationContext源码解析四]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BAnnotationConfigApplicationContext_5.md
[AnnotationConfigApplicationContext源码解析五]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BAnnotationConfigApplicationContext_6.md
[AnnotationConfigApplicationContext源码解析六]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BAnnotationConfigApplicationContext_7.md


[Spring容器之Resource 和 ResourceLoader]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/impl/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BResource%E4%B8%8EResourceLoader.md

[Spring中Bean的描述BeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/Spring%E4%B9%8B%E5%AF%B9%E8%B1%A1%E6%8F%8F%E8%BF%B0BeanDefinition.md
[BeanDefinitionHolder]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/Spring%E4%B8%AD%E7%9A%84BeanDefinitionHolder.md

[AbstractBeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/beanDefinition/AbstractBeanDefinition.md
[RootBeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/beanDefinition/RootBeanDefinition.md
[ChildBeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/beanDefinition/ChildBeanDefinition.md
[GenericBeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/beanDefinition/RootBeanDefinition.md
[AnnotatedGenericBeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/beanDefinition/AnnotatedGenericBeanDefinition.md
[ScannedGenericBeanDefinition]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/beanDefinition/AnnotatedGenericBeanDefinition.md



[spring中的依赖注入脉络图]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/Spring%E4%B9%8B%E4%BE%9D%E8%B5%96%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E7%9A%84%E8%84%89%E7%BB%9C.md
[Spring依赖注入之getBean()]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/Springy%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BgetBean().md
[getBean()之doGetBean源码阅读一]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/dogetbean/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5doGetBean()_1.md
[getBean()之doGetBean源码阅读二]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/dogetbean/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5doGetBean()_2.md

[`singleton` Bean的实例化过程]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean().md
[`prototype` Bean的实例化过程]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_1.md
[`scope` Bean的实例化过程]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_2.md
[准备创建 bean]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_3.md

[doCreateBean预览]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_4.md
[createBeanInstance实现]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_5.md

[使用工厂方法对Bean进行实例化]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_6.md
[通过构造器的方式注入对象]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_7.md
[默认的实例化 `instantiateBean()`]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_8.md
[MergedBeanDefinitionPostProcessor 的应用]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_9.md
[Bean的依赖关系处理]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_10.md
[将原生对象变成代理对象]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_11.md
[自动装配的实现]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_12.md
[applyPropertyValues]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_13.md



[开头]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/aop/src/Aop%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB_01.md
[创建Aop代理]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/aop/src/Aop%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB_02.md
[获取增强器]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/aop/src/Aop%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB_03.md

