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

#### ApplicationContext
### IOC 容器的初始化过程
从下面这段代码开始：
``` java
AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(MyConfig.class);
```



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
