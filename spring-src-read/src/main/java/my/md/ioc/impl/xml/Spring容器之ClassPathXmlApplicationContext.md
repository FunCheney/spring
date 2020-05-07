### ClassPathXmlApplicationContext

#### 使用
```java
public class ClassPathApplicationContextTest {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext xmlAppContext = new ClassPathXmlApplicationContext("spring-bean.xml");
		ClassPathAppContextService service = (ClassPathAppContextService)xmlAppContext.getBean("classPathAppContextService");
		service.test();
	}
}
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans
        https://www.springframework.org/schema/beans/spring-beans.xsd">

	<bean id="classPathAppContextService" class="com.fchen.service.ClassPathAppContextService">
		<!-- collaborators and configuration for this bean go here -->
	</bean>

	<bean id="xmlServiceTest" class="com.fchen.service.XmlBeanFactoryService">
		<!-- collaborators and configuration for this bean go here -->
	</bean>
</beans>
```
new ClassPathXmlApplicationContext("spring-bean.xml")

断点调用逻辑：

时序图：




