### 开头

```java
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	/**
	 * AopNamespaceHandler init 方法注册自定义的解析器
	 */
	@Override
	public void init() {
		// In 2.0 XSD as well as in 2.1 XSD.
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
		// 遇到 aspectj-autoproxy 注解时 就会使用解析器 AspectJAutoProxyBeanDefinitionParser 进行解析
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace as of 2.1
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}
```

#### 注册 AnnotationAwareAspectJAutoProxyCreator
&ensp;&ensp;所有解析器，是因为对 BeanDefinitionParser 接口的统一实现，入口都是从 parse 函数开始的，
AspectJAutoProxyBeanDefinitionParser 中的 parse 的函数如下：

```java
public BeanDefinition parse(Element element, ParserContext parserContext) {
    // 第一部分
    // 注册 AnnotationAwareAspectJAutoProxyCreator
    AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
    // 第二部分
    // 对于注解中子类的处理
    extendBeanDefinition(element, parserContext);
    return null;
}
```

&ensp;&ensp;上述代码中第一部分的代码逻辑是关键代码，是主要代码的实现。

```java
public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
        ParserContext parserContext, Element sourceElement) {

    // 注册或升级 AutoProxyCreator 定义 beanName 
    // 为 org.springframework.aop.config.internalAutoProxyCreator
    // 的 BeanDefinition
    BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
            parserContext.getRegistry(), parserContext.extractSource(sourceElement));
    // 对于 proxy-target-class 以及 expose-proxy 的处理
    useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);

    /*
     * 注册组件并通知，便于监听器做进一步处理
     * 其中 beanDefinition 的 className 为 AnnotationAwareAspectJAutoProxyCreator
     */
    registerComponentIfNecessary(beanDefinition, parserContext);
}
```
&ensp;&ensp;registerComponentIfNecessary 方法中主要完成了3件事，每一行代码都是一个字逻辑的实现：

1. 注册或升级 AspectJAnnotationAutoProxyCreator

```java
public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
        BeanDefinitionRegistry registry, @Nullable Object source) {

    return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
}
```

```java
private static BeanDefinition registerOrEscalateApcAsRequired(
        Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

    /*
      如果已经存在了自动代理创建器且存在的自动代理创建器与现在的不一致
      那么需要根据优先级来判断到底使用哪个
     */
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
        BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
        if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
            int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
            int requiredPriority = findPriorityForClass(cls);
            if (currentPriority < requiredPriority) {
                // 改变bean 最重要的就是改变 bean 所对应的 className 属性
                apcDefinition.setBeanClassName(cls.getName());
            }
        }
        // 如果已经存在自动代理创建器并且与将要创建的一致，那么无需在创建
        return null;
    }

    RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
    beanDefinition.setSource(source);
    beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
    return beanDefinition;
}
```

2. 对于 proxy-target-class 以及 expose-proxy 的处理


3. registerComponentIfNecessary




