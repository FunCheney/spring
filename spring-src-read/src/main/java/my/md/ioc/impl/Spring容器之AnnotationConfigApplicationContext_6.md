## Spring容器初始化 refresh() 方法_03
&ensp;&ensp;上篇文章中讲到`invokeBeanFactoryPostProcessor()`中有一个非常重要分方法`parse()`，今天这篇文章中，就来看看这个方法的实现。开始之前，通过下图回顾
一下：
![解析配置候选类](https://imgkr.cn-bj.ufileos.com/c2c15c7f-8276-471e-96b6-4eb1fa506130.png)
## 5.7 ConfigurationClassParser#parse(Set<BeanDefinitionHolder>)
&ensp;&ensp;方法入口如下： 
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
![MyConfig的BD](https://imgkr.cn-bj.ufileos.com/ca12748e-d068-4774-acd5-a4432cb092c6.png)
&ensp;&ensp;按照上图，进入对应的 `prase()` 方法如下，在该方法中调用对应的 `processConfigurationClas()` 方法，完成对 `ConfigurationClass`
的处理。

```
protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
    processConfigurationClass(new ConfigurationClass(metadata, beanName));
}
```
&esnp;&ensp; `processConfigurationClass()` 方法的处理比较复杂，我们先通过流程图简单的看看该方法都做了什么，人后逐个击破：
![processConfigurationClass流程](https://imgkr.cn-bj.ufileos.com/e035c0a5-7f97-4e2e-b21d-1038fa79d006.png)
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
     * 提取 @Bean 方法 信息
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

### 1. @Configuration 中对内部类的处理

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
#### 1.1 判断是否为配置候选类
```java
public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
    // 判断是是否为全配置候选类 || 判断是否为部分配置候选类
    return (isFullConfigurationCandidate(metadata) || isLiteConfigurationCandidate(metadata));
}
```
#### 1.2 判断是否为全配置候选类（Full）
```java
public static boolean isFullConfigurationCandidate(AnnotationMetadata metadata) {
    return metadata.isAnnotated(Configuration.class.getName());
}
```
#### 1.3 判断是否为部分配置候选类（Lite）
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
#### 1.3 Full 与 Lite 配置类的区别
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
&ensp;&ensp;最后对于内部类的处理方式，也是通过调用 `processConfigurationClass()` 方法来完成相应的处理。也就是说，先经过一系列的判断，将符合
条件的加入到候选类治类的集合中 `List<SourceClass> candidates`。然后在for循序，拿出每一个配置类，进行相应的处理。

### 2. @Configuration 中对 @PropertySource 注解的处理
&ensp;&ensp;在Spring 中通过 `@PropertySource` 来加载指定的属性文件。
```java
private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
    String name = propertySource.getString("name");
    if (!StringUtils.hasLength(name)) {
        name = null;
    }
    String encoding = propertySource.getString("encoding");
    if (!StringUtils.hasLength(encoding)) {
        encoding = null;
    }
    String[] locations = propertySource.getStringArray("value");
    Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
    boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

    // 得到创建 PropertySource的工厂
    Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
    //创建 PropertySource的工厂如果是 PropertySourceFactory 就使用Spring 内部默认的 实现 DefaultPropertySourceFactory
    //否则 通过反射创建一个对象
    PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ?
            DEFAULT_PROPERTY_SOURCE_FACTORY : BeanUtils.instantiateClass(factoryClass));

    for (String location : locations) {
        try {
            String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
            Resource resource = this.resourceLoader.getResource(resolvedLocation);
            // 调用factory的createPropertySource方法根据名字、编码、资源创建出一个PropertySource出来
            addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
        }
        catch (IllegalArgumentException | FileNotFoundException | UnknownHostException ex) {
            // Placeholders not resolvable or resource not found when trying to open it
            if (ignoreResourceNotFound) {
                if (logger.isInfoEnabled()) {
                    logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
                }
            }
            else {
                throw ex;
            }
        }
    }
}
```

```java
private void addPropertySource(PropertySource<?> propertySource) {
    String name = propertySource.getName();
    MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment).getPropertySources();

    if (this.propertySourceNames.contains(name)) {
        // We've already added a version, we need to extend it
        PropertySource<?> existing = propertySources.get(name);
        if (existing != null) {
            PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ?
                    ((ResourcePropertySource) propertySource).withResourceName() : propertySource);
            if (existing instanceof CompositePropertySource) {
                ((CompositePropertySource) existing).addFirstPropertySource(newSource);
            }
            else {
                if (existing instanceof ResourcePropertySource) {
                    existing = ((ResourcePropertySource) existing).withResourceName();
                }
                CompositePropertySource composite = new CompositePropertySource(name);
                composite.addPropertySource(newSource);
                composite.addPropertySource(existing);
                propertySources.replace(name, composite);
            }
            return;
        }
    }

    if (this.propertySourceNames.isEmpty()) {
        propertySources.addLast(propertySource);
    }
    else {
        String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
        propertySources.addBefore(firstProcessed, propertySource);
    }
    this.propertySourceNames.add(name);
}
```
&ensp;&ensp;我在这里暂时没有用到， `@PropertySource` 注解，这里先指出代码的处理，后面用到的时候，在做详细的介绍。这里，我们先对这部分内容过掉。

### 3. @Configuration 中对 @ComponentScan 注解的处理
```java
public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, final String declaringClass) {
    /**
     * new 一个 ClassPathBeanDefinitionScanner 的 scanner 扫描包
     */
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
            componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);

    Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
    boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
    scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :
            BeanUtils.instantiateClass(generatorClass));

    ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
    if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
        scanner.setScopedProxyMode(scopedProxyMode);
    }
    else {
        Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
        scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
    }

    scanner.setResourcePattern(componentScan.getString("resourcePattern"));

    for (AnnotationAttributes filter : componentScan.getAnnotationArray("includeFilters")) {
        for (TypeFilter typeFilter : typeFiltersFor(filter)) {
            scanner.addIncludeFilter(typeFilter);
        }
    }
    for (AnnotationAttributes filter : componentScan.getAnnotationArray("excludeFilters")) {
        for (TypeFilter typeFilter : typeFiltersFor(filter)) {
            scanner.addExcludeFilter(typeFilter);
        }
    }

    boolean lazyInit = componentScan.getBoolean("lazyInit");
    if (lazyInit) {
        scanner.getBeanDefinitionDefaults().setLazyInit(true);
    }

    Set<String> basePackages = new LinkedHashSet<>();
    String[] basePackagesArray = componentScan.getStringArray("basePackages");
    for (String pkg : basePackagesArray) {
        String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Collections.addAll(basePackages, tokenized);
    }
    for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
        basePackages.add(ClassUtils.getPackageName(clazz));
    }
    if (basePackages.isEmpty()) {
        basePackages.add(ClassUtils.getPackageName(declaringClass));
    }

    scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
        @Override
        protected boolean matchClassName(String className) {
            return declaringClass.equals(className);
        }
    });
    /**
     * 在这里做doScan()
     */
    return scanner.doScan(StringUtils.toStringArray(basePackages));
}
```
&ensp;&ensp;从这里可以看出，对于包的扫描，是在扫描的时候 `new ClassPathBeanDefinitionScanner()` 创建的扫描器来完成扫描的，并不是使用
`AnnotationConfigApplicationContext` 中初始化的扫描器来完成包扫描的。
#### 3.1 doScan()
```java
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    for (String basePackage : basePackages) {
        /**
         * 扫描basePackage路径下的 java 文件
         * 将其转换为BeanDefinition
         */
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);


        for (BeanDefinition candidate : candidates) {
            ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
            candidate.setScope(scopeMetadata.getScopeName());
            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
            /**
             * 如果 candidate 是 AbstractBeanDefinition 的子类
             */
            if (candidate instanceof AbstractBeanDefinition) {
                /** 为 candidate 设置默认值*/
                postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
            }
            if (candidate instanceof AnnotatedBeanDefinition) {
                /**
                 * 如果 candidate 是 AnnotatedBeanDefinition 的子类
                 * 检查并处理常用的注解，把值设置到 AnnotatedBeanDefinition 中
                 * 这里只有被加了注解的类才会被处理
                 */
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
            if (checkCandidate(beanName, candidate)) {
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                definitionHolder =
                        AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                beanDefinitions.add(definitionHolder);
                /** 加入到 Map 当中*/
                registerBeanDefinition(definitionHolder, this.registry);
            }
        }
    }
    return beanDefinitions;
}
```
  &ensp;&ensp;又是熟悉的`doXXX()`方法，通过这个方法，将我们定义的包路径下的对象添加到IoC容器中去当然，在注册到容器中之前，也要对扫描得到的`BeanDeifition`的属性进行处理，如通用的注解
#### 3.2 通用注解的处理
```java
/**
 * 处理类的通用注解
 * @param abd spring中bean的描述类
 */
public static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd) {
    processCommonDefinitionAnnotations(abd, abd.getMetadata());
}

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

#### 3.3 对扫描出来的类注册
```java
if (checkCandidate(beanName, candidate)) {
    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
    definitionHolder =
            AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
    beanDefinitions.add(definitionHolder);
    /** 加入到 Map 当中*/
    registerBeanDefinition(definitionHolder, this.registry);
}
```
&ensp;&ensp;通过这里，就可以将我们自己交给Spring管理的类，注册到了容器当中，对应的我们的容器形成图例里面的内容也相应的增加了：
##### 3.3.1容器中的对象
![容器中的对象](https://imgkr.cn-bj.ufileos.com/52b4644e-2757-4e78-8f8c-c5891de85d2d.png)
##### 3.3.2容器中的对象名称
![容器中的对象名称](https://imgkr.cn-bj.ufileos.com/9057bc69-6b8f-4456-a0bf-cb4381e3757e.png)
##### 3.3.3容器形成图
![容器形成图](https://imgkr.cn-bj.ufileos.com/a7b734a4-f4da-4da0-8cf7-10bfdc05a6eb.png)


### 4. @Configuration 中对 @Import(xxx.class) 的处理
```java
private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
        Collection<SourceClass> importCandidates, boolean checkForCircularImports) {

    if (importCandidates.isEmpty()) {
        return;
    }

    if (checkForCircularImports && isChainedImportOnStack(configClass)) {
        this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
    }
    else {
        this.importStack.push(configClass);
        try {
            for (SourceClass candidate : importCandidates) {

                if (candidate.isAssignable(ImportSelector.class)) {
                    // Candidate class is an ImportSelector -> delegate to it to determine imports
                    Class<?> candidateClass = candidate.loadClass();
                    /** 反射一个对象实现 */
                    ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
                    ParserStrategyUtils.invokeAwareMethods(
                            selector, this.environment, this.resourceLoader, this.registry);
                    if (selector instanceof DeferredImportSelector) {
                        this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
                    }
                    else {
                        /** 回调*/
                        String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
                        /**
                         * 递归，这里第二次调用 processImports
                         * 如果是一个普通的类 会进 else
                         */
                        Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
                        processImports(configClass, currentSourceClass, importSourceClasses, false);
                    }
                }
                else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
                    /**
                     * 判断Import的是不是 ImportBeanDefinitionRegistrar
                     */
                    // Candidate class is an ImportBeanDefinitionRegistrar ->
                    // delegate to it to register additional bean definitions
                    Class<?> candidateClass = candidate.loadClass();
                    ImportBeanDefinitionRegistrar registrar =
                            BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
                    ParserStrategyUtils.invokeAwareMethods(
                            registrar, this.environment, this.resourceLoader, this.registry);
                    configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
                }
                else {
                    // Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
                    // process it as an @Configuration class
                    /**
                     * 普通类的处理方式
                     * 加入到 importStack 后调用 processConfigurationClass 进行处理
                     * processConfigurationClass 里面主要就是把类放到 configurationClasses 中
                     * configurationClasses 是一个集合 会在后面拿出来解析成bd 接续注册
                     * 注意：
                     *    普通类实在扫描出来的时候就被注册了
                     *    importSelector 会放到 configurationClasses 中 然后进行注册
                     */
                    this.importStack.registerImport(
                            currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
                    processConfigurationClass(candidate.asConfigClass(configClass));
                }
            }
        }
        catch (BeanDefinitionStoreException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new BeanDefinitionStoreException(
                    "Failed to process import candidates for configuration class [" +
                    configClass.getMetadata().getClassName() + "]", ex);
        }
        finally {
            this.importStack.pop();
        }
    }
}
```

### 5. @Configuration 中对 @Bean 方法 信息的提取
&ensp;&ensp;获取当前类中 `@Bean` 注解的方法的元数据，包含比如方法名、所在类全名、返回类型、是否静态、是否不可覆盖等等信息。
```java
private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
    /** 获取类的元数据*/
    AnnotationMetadata original = sourceClass.getMetadata();
    /** 获取所有@Bean注解的方法*/
    Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
    /** 判断方法集合是否超过两个，并且类的元数据是StandardAnnotationMetadata实例，则从ASM内获取声明的方法顺序*/
    if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
        // Try reading the class file via ASM for deterministic declaration order...
        // Unfortunately, the JVM's standard reflection returns methods in arbitrary
        // order, even between different runs of the same application on the same JVM.
        try {
            AnnotationMetadata asm =
                    this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
            Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
            if (asmMethods.size() >= beanMethods.size()) {
                Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
                for (MethodMetadata asmMethod : asmMethods) {
                    for (MethodMetadata beanMethod : beanMethods) {
                        if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
                            selectedMethods.add(beanMethod);
                            break;
                        }
                    }
                }
                if (selectedMethods.size() == beanMethods.size()) {
                    // All reflection-detected methods found in ASM method set -> proceed
                    beanMethods = selectedMethods;
                }
            }
        }
        catch (IOException ex) {
            logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
            // No worries, let's continue with the reflection metadata we started with...
        }
    }
    return beanMethods;
}
```

