## AnnotationConfigApplicationContext 源码解析 注册配置类
### 入口代码展示
```
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
    /**
     * 调用默认的构造方法，由于该类有父类，
     * 故而先调用父类的构造方法，在调用自己的构造方法
     * 在自己的构造方法中初始一个读取器和一个扫描器，
     * 第①部分！！！
     */
    this();
    /**
     * 向spring 的容器中注册bean
     * 第②部分！！！
     */
    register(componentClasses);
    /**
     * 初始化spring的环境
     * 第③部分！！！
     */
    refresh();
}
```
&ensp;&ensp;这里将要解析上述代码中的`register(componentClasses)`这一部分的代码。。。
### 注册配置类

```java
/**
 * 注册单个bean给容器
 * 比如有新加的类可以用这个方法
 * 但是注册之后需要手动调用refresh()方法触发容器解释注释
 *
 * 可以注册一个配置类
 * 也可以注册一个bean
 */
@Override
public void register(Class<?>... componentClasses) {
    Assert.notEmpty(componentClasses, "At least one component class must be specified");
    this.reader.register(componentClasses);
}
```
&ensp;&ensp; 上述代码中通过 `this.reader.register(componentClasses)` 完成了配置类的注册，这里的 `reader` 对象就是上一篇文章中提到的
bean的读取器 `AnnotatedBeanDefinitionReader`。
```java
public void register(Class<?>... componentClasses) {
    for (Class<?> componentClass : componentClasses) {
        registerBean(componentClass);
    }
}
```
### 初识 Spring 的 doXXX方法
&ensp;&ensp;在Spring当中真正的做事情的方法都是通过 `do` 来开头完成的。在后续的方法中，会看到很多这类的方法。这里首先看看 `doRegisterBean()`
方法实现：
```java
<T> void doRegisterBean(Class<T> beanClass, @Nullable Supplier<T> instanceSupplier, @Nullable String name,
        @Nullable Class<? extends Annotation>[] qualifiers, BeanDefinitionCustomizer... definitionCustomizers) {
    /*
     * 根据指定的bean创建一个AnnotatedGenericBeanDefinition
     * 这个AnnotatedGenericBeanDefinition可以理解为一个数据结构，
     * 该结构中包含了类的一些描述信息。比如scope，lazy等等
     */
    AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
    if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
        return;
    }

    abd.setInstanceSupplier(instanceSupplier);
    ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
    /*
     * 设置类的作用域
     */
    abd.setScope(scopeMetadata.getScopeName());
    /*
     * 通过beanNameGenerator生成一个beanName
     */
    String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));

    /*
     * 处理类当中的通用注解
     * 处理完的数据 存放在 abd 中
     */
    AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);

    /*
     * 当 qualifiers 不为null 时，处理qualifiers
     */
    if (qualifiers != null) {
        for (Class<? extends Annotation> qualifier : qualifiers) {
            /** 如果设置了 @Primary 则将其值设置为true */
            if (Primary.class == qualifier) {
                abd.setPrimary(true);
            }
            else if (Lazy.class == qualifier) {
                /** 如果设置了 @Lazy 则将其值设置为true */
                abd.setLazyInit(true);
            }
            else {
                /**
                 * 使用了其他注解，则为该bean添加一个根据名字自动装配的限定符
                 */
                abd.addQualifier(new AutowireCandidateQualifier(qualifier));
            }
        }
    }
    for (BeanDefinitionCustomizer customizer : definitionCustomizers) {
        customizer.customize(abd);
    }

    /*
     * BeanDefinitionHolder 也是一种数据结构
     * 将beanName 与 abd 关联
     */
    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);

    /*
     * ScopedProxyMode 结合web去理解
     */
    definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
    /*
     * 将definitionHolder 注册给 registry
     * registry 是 AnnotationConfigApplicationContext
     * AnnotationConfigApplicationContext 在初始化的时候通过调用父类的构造方法实例化一个 DefaultListableBeanFactory
     *
     *   registerBeanDefinition 就是将 definitionHolder 注册到 DefaultListableBeanFactory中
     */
    BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
}
```
### doRegisterBean 时序图 
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotationConfigApplicationContext_register.jpg">
 </div>

&ensp;&ensp;上述改方法的调用过程，最终就是将定义的MyConfig类对应的BeanDefinition放入到beanDefinitionMap中，
至此beanDefinitionMap中对象又增加了一个，变成6个了。

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/beanDefinitionMap_six_object.jpg">
 </div>
 
 #### 处理通用注解
 ```java
/**
 *  处理类的通用注解
 * @param abd spring中bean的描述类
 * @param metadata 通过spring中bean的描述类获取 bean的元数据信息
 *
 *       处理完通用注解后的信息 放回到 spring中bean的描述类(AnnotatedBeanDefinition)
 */
static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd, AnnotatedTypeMetadata metadata) {
    /**
     * 处理 @Lazy 注解
     */
    AnnotationAttributes lazy = attributesFor(metadata, Lazy.class);
    if (lazy != null) {
        /** 设置bean的懒加载信息*/
        abd.setLazyInit(lazy.getBoolean("value"));
    }
    else if (abd.getMetadata() != metadata) {
        lazy = attributesFor(abd.getMetadata(), Lazy.class);
        if (lazy != null) {
            abd.setLazyInit(lazy.getBoolean("value"));
        }
    }

    /**
     * 处理 @Primary 注解
     */
    if (metadata.isAnnotated(Primary.class.getName())) {
        abd.setPrimary(true);
    }

    /**
     * 处理 @DependsOn 注解
     */
    AnnotationAttributes dependsOn = attributesFor(metadata, DependsOn.class);
    if (dependsOn != null) {
        abd.setDependsOn(dependsOn.getStringArray("value"));
    }

    /**
     * 处理 @Role 注解
     */
    AnnotationAttributes role = attributesFor(metadata, Role.class);
    if (role != null) {
        abd.setRole(role.getNumber("value").intValue());
    }

    /**
     * 处理 @Description注解
     */
    AnnotationAttributes description = attributesFor(metadata, Description.class);
    if (description != null) {
        abd.setDescription(description.getString("value"));
    }
}
```

#### 将 `definitionHolder` 注册给 `registry`
&ensp;&ensp;`registry` 是 `AnnotationConfigApplicationContext`，因为 `AnnotationConfigApplicationContext
` 是 `BeanDefinitionRegistry` 的实现类。
```java
public static void registerBeanDefinition(
        BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
        throws BeanDefinitionStoreException {

    // Register bean definition under primary name.
    /** 获取beanName */
    String beanName = definitionHolder.getBeanName();
    /**
     * 注册 beanDefinition, beanName 与 BeanDefinition 放入Map 中
     */
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

    // Register aliases for bean name, if any.
    /** 处理别名 */
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
        for (String alias : aliases) {
            registry.registerAlias(beanName, alias);
        }
    }
}
```
 
 ### 容器形成图
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/annotionConfigApplication/spring_ioc_contains_2.jpg">
 </div>
 
 &ensp;&ensp;通过上述过程发现，定义的配置类被加载到了容器之中，这样一来容器中的对象又多了一个。这里对容器中对象的名称，
 即 `BeanDefinitionMap` 中的 `key` 做一个简要的说明。
 
 * Spring 内部的 `BeanDefinition` 的 `name` 是在Spring 内部写死的，对应的 `BeanDefinition` 的实现是 `RootBeanDefinition` 。
 * 注入的配置类，如果有指定的名称，则使用指定的名称，如果没有指定，则使用类名。对应的代码如下：
 
 ```java
public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
    if (definition instanceof AnnotatedBeanDefinition) {
        // 通过注接确定 bean 的名称
        String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
        if (StringUtils.hasText(beanName)) {
            // Explicit bean name found.
            return beanName;
        }
    }
    // 生成一个默认的唯一的beanName
    return buildDefaultBeanName(definition, registry);
}
```

```java
protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
    // 获取类上的注解
    AnnotationMetadata amd = annotatedDef.getMetadata();
    // 获取注解的类型
    Set<String> types = amd.getAnnotationTypes();
    String beanName = null;
    for (String type : types) {
        AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
        if (attributes != null && isStereotypeWithNameValue(type, amd.getMetaAnnotationTypes(type), attributes)) {
            // 获取value属性
            Object value = attributes.get("value");
            // 如果是 字符串 则beanName 就是指定的，否则为 null
            if (value instanceof String) {
                String strVal = (String) value;
                if (StringUtils.hasLength(strVal)) {
                    if (beanName != null && !strVal.equals(beanName)) {
                        throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
                                "component names: '" + beanName + "' versus '" + strVal + "'");
                    }
                    beanName = strVal;
                }
            }
        }
    }
    return beanName;
}
```
&ensp;&ensp;下面是生成一个默认的 `beanName`:
```java
protected String buildDefaultBeanName(BeanDefinition definition) {
    String beanClassName = definition.getBeanClassName();
    Assert.state(beanClassName != null, "No bean class name set");
    String shortClassName = ClassUtils.getShortName(beanClassName);
    return Introspector.decapitalize(shortClassName);
}
```

&ensp;&ensp;在上述图中有两个对象， `beanNameGenerator` 和 `scopeMetadataResolver` 分别为 `beanName` 生成器和 `bean` 定义范围解析器。
在Spring中这两个对象对应两个 `BeanNameGenerator` 和 `ScopeMetadataResolver` 策略接口。真正的处理逻辑在实现类中完成，这里对应策略的获取，
都是通过 `new` 实现类的方式来完成的。在 `AnnotatedBeanDefinitionReader` 中对应的代码如下：
```java
private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
```


 