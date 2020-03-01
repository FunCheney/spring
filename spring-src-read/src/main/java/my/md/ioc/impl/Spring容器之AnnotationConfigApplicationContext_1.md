### AnnotationConfigApplicationContext 的使用
#### 1.启动类
使用方式一：

```java
public class MyTestStart {
	public static void main(String[] args) {
		ApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		MyService myService = ann.getBean(MyService.class);
		myService.test();
	}
}
```
使用方式二：


#### 2.配置类
```java
@Configuration
@ComponentScan("com.fchen")
public class MyConfig {
	
}
```
#### 2.Service类
```java
@Service
public class MyService {

	private Logger log = LoggerFactory.getLogger(MyService.class);
	public void test(){
		System.out.println("Hello,MyService");
	}
}
```


### 2.Spring 提供的两个扩展点的使用
 
#### 2.1 BeanPostProcessor

#### 2.2 BeanFactoryPostProcessor 

- spring内部的 BeanFactoryPostProcessor

- 程序员自定义的 BeanFactoryPostProcessor

- 程序员在定义的 且交给Spring管理的