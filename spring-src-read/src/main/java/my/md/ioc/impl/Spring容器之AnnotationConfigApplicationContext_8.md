##  Spring容器初始化 refresh() 方法_05

### finishBeanFactoryInitialization
```java
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    // Initialize conversion service for this context.
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
            beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
        beanFactory.setConversionService(
                beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

    // Register a default embedded value resolver if no bean post-processor
    // (such as a PropertyPlaceholderConfigurer bean) registered any before:
    // at this point, primarily for resolution in annotation attribute values.
    if (!beanFactory.hasEmbeddedValueResolver()) {
        beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
    }

    // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
    String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
    for (String weaverAwareName : weaverAwareNames) {
        getBean(weaverAwareName);
    }

    // Stop using the temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(null);

    // Allow for caching all bean definition metadata, not expecting further changes.
    /**
     * 冻结 所有的bean定义，说明注册的bean定义将不被修改或任何进一步的处理
     */
    beanFactory.freezeConfiguration();

    // Instantiate all remaining (non-lazy-init) singletons.
    /**
     * 这里是重点代码
     * 初始化剩下的实例(非惰性的)
     * 这个 预实例化的完成 委托给容器来实现，如果需要预实例化，采用getBean() 触发依赖注入，
     * 与正常的依赖注入的触发相比，只有触发的时间和场合不同，这里依赖注入是发生在容器执行 refresh() 的过程，也就是IOC容器初始化的过程
     * 正常不是 Lazy-init 属性的依赖注入发生在IOC容器初始化完成之后，第一次向容器执行getBean() 时。
     */
    beanFactory.preInstantiateSingletons();
}
```
```java
public void preInstantiateSingletons() throws BeansException {
    if (logger.isTraceEnabled()) {
        logger.trace("Pre-instantiating singletons in " + this);
    }

    // Iterate over a copy to allow for init methods which in turn register new bean definitions.
    // While this may not be part of the regular factory bootstrap, it does otherwise work fine.
    // 获取所有BeanDefinition的名字
    List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

    // Trigger initialization of all non-lazy singleton beans...
    // 触发所有非延迟加载单例Bean的实例化
    for (String beanName : beanNames) {
        // 合并父BeanDefinition
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        /**
         * 一个对象不是抽象的，并且是单例的，并且不是懒加载的
         * 若这个对象不是一个FactoryBean，进入getBean()方法
         */
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            if (isFactoryBean(beanName)) {
                // 如果是FactoryBean 加上 &
                Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
                if (bean instanceof FactoryBean) {
                    final FactoryBean<?> factory = (FactoryBean<?>) bean;
                    boolean isEagerInit;
                    if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                        isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>)
                                        ((SmartFactoryBean<?>) factory)::isEagerInit,
                                getAccessControlContext());
                    }
                    else {
                        isEagerInit = (factory instanceof SmartFactoryBean &&
                                ((SmartFactoryBean<?>) factory).isEagerInit());
                    }
                    if (isEagerInit) {
                        getBean(beanName);
                    }
                }
            }
            else {
                /**
                 * 调用容器中的 getBean() 方法
                 */
                getBean(beanName);
            }
        }
    }

    // Trigger post-initialization callback for all applicable beans...
    for (String beanName : beanNames) {
        Object singletonInstance = getSingleton(beanName);
        if (singletonInstance instanceof SmartInitializingSingleton) {
            final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    smartSingleton.afterSingletonsInstantiated();
                    return null;
                }, getAccessControlContext());
            }
            else {
                smartSingleton.afterSingletonsInstantiated();
            }
        }
    }
}
```

```java
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
        @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

    // 转换beanName, FactoryBean 的name会已& 开头
    final String beanName = transformedBeanName(name);
    Object bean;

    // Eagerly check singleton cache for manually registered singletons.
    /**
     * 先从缓存中取得Bean，处理那些已经被创建过的单例模式的Bean，这些Bean无需重复创建
     * 创建单例Bean的时候，会存在依赖注入的情况，创建依赖的时候为了避免循环依赖
     * spring 创建Bean的原则是不等Bean创建完就会将创建Bean的ObjectFactory提早曝光
     * 也就是将ObjectFactory加入待缓存中，一旦下一个Bean创建的时候需要依赖上个Bean则
     * 直接使用ObjectFactory
     */
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        if (logger.isTraceEnabled()) {
            if (isSingletonCurrentlyInCreation(beanName)) {
                logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                        "' that is not fully initialized yet - a consequence of a circular reference");
            }
            else {
                logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
            }
        }
        /**
         * getObjectForBeanInstance() 完成的是FactoryBean的相关处理，以取得FactoryBean的生产结果，
         * BeanFactory 和 FactoryBean的区别 查看之前的解释
         * 存在BeanFactory的情况并不是直接返回实例本身，而是返回指定方法返回的实例
         */
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }

    else {
        // Fail if we're already creating this bean instance:
        // We're assumably within a circular reference.
        /**
         * 当前正在创建的类是否是prototype，如果是就报错
         * 只有在单例情况下才会尝试解决循环依赖
         */
        if (isPrototypeCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }

        // Check if bean definition exists in this factory.
        /**
         * 判断IOC容器中的 BeanDefinition 是否存在，检查是否能在当前的BeanFactory中取得需要的Bean，
         * 如果当前的 BeanFactory 中取不到，则到双亲的 BeanFactory 中去取；
         * 如果当前的双亲工厂中取不到，就顺着双亲BeanFactory 链一直向上查找
         */
        BeanFactory parentBeanFactory = getParentBeanFactory();
        // parentBeanFactory 不为空 且  beanDefinitionMap 中不存在该name的 BeanDefinition
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // 确定原始 beanName
            String nameToLookup = originalBeanName(name);
            // 若为 AbstractBeanFactory 类型，委托父类处理
            if (parentBeanFactory instanceof AbstractBeanFactory) {
                return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                        nameToLookup, requiredType, args, typeCheckOnly);
            }
            // 递归到BeanFactory中寻找
            else if (args != null) {
                // Delegation to parent with explicit args.
                return (T) parentBeanFactory.getBean(nameToLookup, args);
            }
            else if (requiredType != null) {
                // No args -> delegate to standard getBean method.
                return parentBeanFactory.getBean(nameToLookup, requiredType);
            }
            else {
                return (T) parentBeanFactory.getBean(nameToLookup);
            }
        }

        // 类型检查
        if (!typeCheckOnly) {
            // 添加到 alreadyCreated 集合中
            markBeanAsCreated(beanName);
        }

        try {
            /**
             * 将存储在XML配置文件的 GernericBeanDefinition转换为RootBeanDefinition
             * 如果指定BeanName 是 子Bean的话，同时会合并父类相关的属性
             * 根据bean的名字 获取 BeanDefinition
             */
            final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

            checkMergedBeanDefinition(mbd, beanName, args);

            /**
             * 获取当前Bean的所有依赖Bean，在getBean()时递归调用。
             * 直到取到一个没有任何依赖的Bean为止
             */
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
                // 如从在依赖则需要递归实例化依赖的Bean
                for (String dep : dependsOn) {
                    // 检验依赖的bean 是否已经注册给当前 bean 获取其他传递依赖bean
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }
                    // 缓存依赖调用
                    registerDependentBean(dep, beanName);
                    try {
                        // 触发新的依赖
                        getBean(dep);
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                    }
                }
            }

            // Create bean instance.
            if (mbd.isSingleton()) {
                /**
                 * 通过createBean()方法创建singleton bean的实例，
                 * 这里通过拉姆达表达式创建 Object 对象
                 * 在getSingleton()中调用ObjectFactory 的createBean
                 */
                sharedInstance = getSingleton(beanName, () -> {
                    try {
                        return createBean(beanName, mbd, args);
                    }
                    catch (BeansException ex) {
                        // Explicitly remove instance from singleton cache: It might have been put there
                        // eagerly by the creation process, to allow for circular reference resolution.
                        // Also remove any beans that received a temporary reference to the bean.
                        destroySingleton(beanName);
                        throw ex;
                    }
                });
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            }

            /** 这里是创建prototype bean的地方*/
            else if (mbd.isPrototype()) {
                // It's a prototype -> create a new instance.
                Object prototypeInstance = null;
                try {
                    beforePrototypeCreation(beanName);
                    prototypeInstance = createBean(beanName, mbd, args);
                }
                finally {
                    afterPrototypeCreation(beanName);
                }
                bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
            }

            else {
                String scopeName = mbd.getScope();
                final Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }
                try {
                    Object scopedInstance = scope.get(beanName, () -> {
                        beforePrototypeCreation(beanName);
                        try {
                            return createBean(beanName, mbd, args);
                        }
                        finally {
                            afterPrototypeCreation(beanName);
                        }
                    });
                    bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                }
                catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName,
                            "Scope '" + scopeName + "' is not active for the current thread; consider " +
                            "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                            ex);
                }
            }
        }
        catch (BeansException ex) {
            cleanupAfterBeanCreationFailure(beanName);
            throw ex;
        }
    }

    // Check if required type matches the type of the actual bean instance.
    /**
     * 这里对创建的Bean进行类型检查，
     * 如果没有问题，就返回这个新创建的Bean
     * 这个Bean 是已经包含了依赖关系的Bean
     */
    if (requiredType != null && !requiredType.isInstance(bean)) {
        try {
            T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
            if (convertedBean == null) {
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
            return convertedBean;
        }
        catch (TypeMismatchException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("Failed to convert bean '" + name + "' to required type '" +
                        ClassUtils.getQualifiedName(requiredType) + "'", ex);
            }
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
    }
    return (T) bean;
}
```
