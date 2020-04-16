### Aop 分析入口

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

&ensp;&ensp;useClassProxyingIfNecessary 实现了 `proxy-target-class` 属性以及 `expose-proxy`
属性的处理。
```java
private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, @Nullable Element sourceElement) {
    if (sourceElement != null) {
        // 对于 proxy-target-class 属性的处理
        boolean proxyTargetClass = Boolean.parseBoolean(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
        if (proxyTargetClass) {
            // 强制使用
            AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
        }
        // 对于 expose-proxy 属性的处理
        boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
        if (exposeProxy) {
            AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
        }
    }
}
```

```java
public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
        BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
        definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
    }
}
```

```java
public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
        BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
        definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
    }
}
```
- [] proxy-target-class: Spring Aop 部分使用 JDK 动态代理 或者 CGLIB 来为目标对象创建代理，如果被代理的目标  
对象实现了至少一个接口，则会使用 JDK 代理，所有该目标类型实现的接口都将被代理，若该目标对象没有实现任何接口，则创建  
一个 CGLIB 代理。如果你希望使用 CGLIB 代理，(例如希望代理目标对象的所有方法，而不只是实现自接口的方法)那也可以，但是  
需要考虑如下两个问题。

&ensp;&ensp;&diams; 无法通知（advise）Final 方法，因为他们不能被重写。

&ensp;&ensp;&diams; 需要将 CGLIB 二进制发行包放在 classpath 下面。

&ensp;&ensp;与之像比较，JDK 本身就提供了动态代理，强制使用 CGLIB 代理需要将 `proxy-target-class` 属性这只为 true。
```xml
<aop:config proxy-target-class="true">
    <!-- other beans defined here... -->
</aop:config>
```

&ensp;&ensp;当需要使用 CGLIB 代理和 `@AspectJ` 自动代理支持，可以按照以下方式设置 `<aop:aspectj-autoproxy>`的
proxy-target-class 属性：
```xml
<aop:aspectj-autoproxy proxy-target-class="true"/>
```
&ensp;&ensp;具体细节的差别在实际使用的过程中，会有不同的体现。

- [] JDK 动态代理：其代理对象必须是某个接口的实现，它是在运行期间，创建一个接口的实现类来完成对目标对象的代理。

- [] CGLIB 代理：实现原理类似于 JDk 动态代理，只是他在运行期间生成的代理对象是针对目标类扩展的子类，CGLIB 是高效
的代码生成包，底层是依靠 ASM 操作字节码实现的。

- [] expose-proxy: 有时候目标对象内部的自我调用将无法实施切面中的自我增强。




3. registerComponentIfNecessary




