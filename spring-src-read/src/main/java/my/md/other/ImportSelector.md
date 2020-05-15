## ImportSelector
### 使用
```java
public class MyTestStart {
	public static void main(String[] args) {

		ApplicationContext ann = new AnnotationConfigApplicationContext(MySelectorImportConfig.class);
		MySelectImportService bean = ann.getBean(MySelectImportService.class);
		bean.test();
	}
}
```
```java
@Configuration
@Import(MyImportSelector.class)
public class MySelectorImportConfig {
}
```
```java
public class MyImportSelector implements ImportSelector {
	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return new String[]{MySelectImportService.class.getName()};
	}
}
```
```java
public class MySelectImportService {

	public void test(){
		System.out.println("test MySelectImportService");
	}
}
```


