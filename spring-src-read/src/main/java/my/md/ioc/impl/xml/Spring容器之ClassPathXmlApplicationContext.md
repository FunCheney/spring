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

```java
public ClassPathXmlApplicationContext(
        String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
        throws BeansException {

    /*
     * 调用父类的构造方法
     */
    super(parent);
    /*
     * 设置路径 将配置文件路径 已数组的形式传入
     * ClassPathXmlApplicationContext 中可以对数组进行解析并进行加载
     */
    setConfigLocations(configLocations);
    if (refresh) {
        refresh();
    }
}
```

断点调用逻辑：
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/ClassPathXmlApplication%20/classPathXmlApplicationContext.png">
 </div>

从上图可以看到，都是一层一层的调用父类的方法，最后在 `AbstractApplicationContext` 的构造方法中通过 `this.resourcePatternResolver = getResourcePatternResolver();` 
来完成 `ResourcePatternResolver resourcePatternResolver` 初始化。这里的 `getResourcePatternResolver()` 通过不同的策略来完成初始初始化
涉及到的策略类如下图所示：







