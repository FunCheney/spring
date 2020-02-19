## AnnotationConfigApplicationContext 
### 使用
#### 1.启动类
```java
public class MyTestStart {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ann = new AnnotationConfigApplicationContext(MyConfig.class);
		MyService myService = ann.getBean(MyService.class);
		myService.test();
	}
}

```
#### 2.配置类
```java
@Configuration
@ComponentScan("com.fchen")
public class MyConfig {
}
```
#### 3.测试类
```java
@Service
public class MyService {

	private Logger log = LoggerFactory.getLogger(MyService.class);
	public void test(){
		log.info("hello test");
	}
}
```

