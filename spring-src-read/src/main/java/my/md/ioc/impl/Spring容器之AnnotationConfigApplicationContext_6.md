## ConfigurationClassParser#parse(Set<BeanDefinitionHolder>)
&ensp;&ensp;上篇文章中讲到`invokeBeanFactoryPostProcessor()`中有一个非常重要分方法`parse()`，今天这篇文章中，就来看看这个方法的实现。开始之前，通过下图回顾
一下：
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/ConfigurationClassParser_parse.png">
 </div>

### 5.7 ConfigurationClassParser#parse(Set<BeanDefinitionHolder>)

#### 时序图
 
```
public void parse(Set<BeanDefinitionHolder> configCandidates) {
    for (BeanDefinitionHolder holder : configCandidates) {
        /** 获取BeanDefinition */
        BeanDefinition bd = holder.getBeanDefinition();
        try {
            /** 根据不同的 BeanDefinition 类型做相应的处理*/
            if (bd instanceof AnnotatedBeanDefinition) {

                parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
            }
            else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
                parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
            }
            else {
                parse(bd.getBeanClassName(), holder.getBeanName());
            }
        }
        catch (BeanDefinitionStoreException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BeanDefinitionStoreException(
                    "Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
        }
    }

    this.deferredImportSelectorHandler.process();
}
```
&ensp;&ensp;这里的 `Set<BeanDefinitionHolder> configCandidates` 中只有一个元素，就是对 `MyConfig` 的表述文件 `BeanDefinition` 的
封装，对应的 `BeanDefinitionHolder`。通过下图可知，这个类是 `AnnotatedBeanDefinition` 的实现

prase_1.jpg

&ensp;&ensp;按照上图，进入对应的 `prase()` 方法如下，在该方法中调用对应的 `processConfigurationClas()` 方法，完成对 `ConfigurationClass`
的处理。

```
protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
    processConfigurationClass(new ConfigurationClass(metadata, beanName));
}
```
```
protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
    if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
        return;
    }

    /**
     * 处理 imported 的情况
     *   就是当前这个注解类有没有被别的类import
     *
     */
    ConfigurationClass existingClass = this.configurationClasses.get(configClass);
    if (existingClass != null) {
        if (configClass.isImported()) {
            if (existingClass.isImported()) {
                existingClass.mergeImportedBy(configClass);
            }
            // Otherwise ignore new imported config class; existing non-imported class overrides it.
            return;
        }
        else {
            // Explicit bean definition found, probably replacing an import.
            // Let's remove the old one and go with the new one.
            this.configurationClasses.remove(configClass);
            this.knownSuperclasses.values().removeIf(configClass::equals);
        }
    }

    // Recursively process the configuration class and its superclass hierarchy.
    /** 将对象类型 由ConfigurationClass 转为 SourceClass*/
    SourceClass sourceClass = asSourceClass(configClass);
    do {
        /** 处理 configClass */
        sourceClass = doProcessConfigurationClass(configClass, sourceClass);
    }
    while (sourceClass != null);

    this.configurationClasses.put(configClass, configClass);
}
```
&ensp;&ensp;在上述方法中，首先判断当前类有没有被别的类 `import`。在本文中，当前的`MyConfig`，没有被 `Import`，因此这里不执行。
然后就是通过 `asSourceClass(configClass)` 将 配置类转化为 `SourceClass` 供后续处理。这里的方法对于整个流程来说不重要，我们还是本着抓主要矛盾
的原则，继续往下看，只需要记住，这里的 `configClass` 转化为 `SourceClass` 在后面的 `doProcessConfigurationClass()` 方法中用到。

```
private SourceClass asSourceClass(ConfigurationClass configurationClass) throws IOException {
    AnnotationMetadata metadata = configurationClass.getMetadata();
    if (metadata instanceof StandardAnnotationMetadata) {
        return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
    }
    return asSourceClass(metadata.getClassName());
}
```

&ensp;&ensp;每当看到 Spring 中 `doXXX` 开头的方法，我就知道重头戏要来了，因为，在Spring中，真正做事的都是通过这个方法来处理的，下面就进入到
万众期待的 `doProcessConfigurationClass()` 里面来一探究竟。。。

```
protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
        throws IOException {

    if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
        // Recursively process any member (nested) classes first
        /** 处理内部类*/
        processMemberClasses(configClass, sourceClass);
    }

    // Process any @PropertySource annotations
    /** 处理 @PropertySource 注解*/
    for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
            sourceClass.getMetadata(), PropertySources.class,
            org.springframework.context.annotation.PropertySource.class)) {
        if (this.environment instanceof ConfigurableEnvironment) {
            processPropertySource(propertySource);
        }
        else {
            logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
                    "]. Reason: Environment must implement ConfigurableEnvironment");
        }
    }

    // Process any @ComponentScan annotations
    /** 处理 @ComponentScan 注解*/
    Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
            sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
    if (!componentScans.isEmpty() &&
            !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
        /** 循环处理 componentScans 中的所有属性*/
        for (AnnotationAttributes componentScan : componentScans) {
            // The config class is annotated with @ComponentScan -> perform the scan immediately
            /**
             * 扫描普通类，spring 内部开始扫描包的方法
             */
            Set<BeanDefinitionHolder> scannedBeanDefinitions =
                    this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
            // Check the set of scanned definitions for any further config classes and parse recursively if needed
            for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
                BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
                if (bdCand == null) {
                    bdCand = holder.getBeanDefinition();
                }
                if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
                    parse(bdCand.getBeanClassName(), holder.getBeanName());
                }
            }
        }
    }

     /**
     * 处理 @Import
     * 判断类中是否有 @Import 注解 如果有 把 @Import 中的值拿出来，是一个类；
     *   比如 @Import(xxx.class), 这里将 xxx 传进去解析
     *   在解析过程中 如果发现是一个 importSelector 那么就回调 selector 的方法
     *   返回一个字符串(类名), 通过字符串得到一个类。然后递归调用本方法来处理这个类
     *
     *
     */
    processImports(configClass, sourceClass, getImports(sourceClass), true);

    // Process any @ImportResource annotations
    AnnotationAttributes importResource =
            AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
    if (importResource != null) {
        String[] resources = importResource.getStringArray("locations");
        Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
        for (String resource : resources) {
            String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
            configClass.addImportedResource(resolvedResource, readerClass);
        }
    }

    /**
     * 处理 @Bean 方法
     */
    Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);

    for (MethodMetadata methodMetadata : beanMethods) {
        configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
    }

    // Process default methods on interfaces
    processInterfaces(configClass, sourceClass);

    // Process superclass, if any
    if (sourceClass.getMetadata().hasSuperClass()) {
        String superclass = sourceClass.getMetadata().getSuperClassName();
        if (superclass != null && !superclass.startsWith("java") &&
                !this.knownSuperclasses.containsKey(superclass)) {
            this.knownSuperclasses.put(superclass, configClass);
            // Superclass found, return its annotation metadata and recurse
            return sourceClass.getSuperClass();
        }
    }

    // No superclass -> processing is complete
    return null;
}
```
&ensp;&ensp;从上述方法中可以看出，主要对配置类中属性一一作了处理，如：内部类、 `@ComponentScan`、`@Important`、`@Bean` 。下面将逐个分析。
#### doProcessConfigurationClass 流程图


#### @Configuration 中对内部类的处理

```java
private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
    // 获取内部类
    Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
    if (!memberClasses.isEmpty()) {
        // 定义 candidates 供后面处理使用
        List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
        for (SourceClass memberClass : memberClasses) {
            // 判断内部类是否为配置候选类 && 内部类的名称 不等于 当前配置类的名称
            if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
                    !memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
                // 添加到 候选的 SourceClass 类集合中
                candidates.add(memberClass);
            }
        }
        OrderComparator.sort(candidates);
        for (SourceClass candidate : candidates) {
            if (this.importStack.contains(configClass)) {
                this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
            }
            else {
                this.importStack.push(configClass);
                try {
                    // 处理配置类
                    processConfigurationClass(candidate.asConfigClass(configClass));
                }
                finally {
                    this.importStack.pop();
                }
            }
        }
    }
}
```
&ensp;&ensp;上述代码中有通过 `ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata())` 来判断，内部类是否为配置
候选类。
```java
public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
    // 判断是是否为全配置候选类 || 判断是否为部分配置候选类
    return (isFullConfigurationCandidate(metadata) || isLiteConfigurationCandidate(metadata));
}
```
```java
public static boolean isFullConfigurationCandidate(AnnotationMetadata metadata) {
    return metadata.isAnnotated(Configuration.class.getName());
}
```
```java
public static boolean isLiteConfigurationCandidate(AnnotationMetadata metadata) {
    // Do not consider an interface or an annotation...
    if (metadata.isInterface()) {
        return false;
    }

    // Any of the typical annotations found?
    /**
     * {@link candidateIndicators}
     * 中 包含 {@link Component}、{@link ComponentScan}、
     *        {@link Import}、{@link ImportResource}
     */
    for (String indicator : candidateIndicators) {
        if (metadata.isAnnotated(indicator)) {
            return true;
        }
    }

    // Finally, let's look for @Bean methods...
    try {
        return metadata.hasAnnotatedMethods(Bean.class.getName());
    }
    catch (Throwable ex) {
        if (logger.isDebugEnabled()) {
            logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
        }
        return false;
    }
}
```
&ensp;&ensp;从上述代码中可以看出，对于内部类中全配置候选类的判断，是通过内部类上的注解来判断的，如果在内部类中加了
`@Configuration` 注解，Spring 判断其为全配置候选类，如没有加注解，但是有`@Bean`或者加了`@Component`、`@ComponentScan`、
`@Import`、`@ImportResource`则将其判断为部分配置候选类。具体的定义，是通过静态代码快的方式，添加到 `candidateIndicators` 中：
```java
static {
		candidateIndicators.add(Component.class.getName());
		candidateIndicators.add(ComponentScan.class.getName());
		candidateIndicators.add(Import.class.getName());
		candidateIndicators.add(ImportResource.class.getName());
	}
```
&ensp;&ensp;最后对于内部类的处理方式，也是通过调用 `processConfigurationClass()` 方法来完成相应的处理。

#### @Configuration 中对 @ComponentScan 注解的处理


#### @Configuration 中对 @Import(xxx.class) 的处理


#### @Configuration 中对 @Bean 方法 的处理