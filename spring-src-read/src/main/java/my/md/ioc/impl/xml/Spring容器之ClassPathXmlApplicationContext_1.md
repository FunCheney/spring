## ClassPathXmlApplicationContext 的源码阅读

```
ClassPathAppContextService service = 
     (ClassPathAppContextService)xmlAppContext.getBean("classPathAppContextService");
```

### `getBean()` 方法

####  `AbstractApplicationContext#getBean()`方法
```java
public Object getBean(String name) throws BeansException {
    assertBeanFactoryActive();
    return getBeanFactory().getBean(name);
}

public Object getBean(String name) throws BeansException {
    return doGetBean(name, null, null, false);
}
```

#### `AbstractBeanFactory#doGetBean`方法
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
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // Not found -> check parent.
            String nameToLookup = originalBeanName(name);
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
            /**
             * 获取当前Bean的所有依赖Bean，在getBean()时递归调用。
             * 直到取到一个没有任何依赖的Bean为止
             */
            checkMergedBeanDefinition(mbd, beanName, args);

            // Guarantee initialization of beans that the current bean depends on.
            // 获取当前Bean的所有依赖
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
                // 如从在依赖则需要递归实例化依赖的Bean
                for (String dep : dependsOn) {
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }
                    // 缓存依赖调用
                    registerDependentBean(dep, beanName);
                    try {
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
#### `DefaultSingletonBeanRegistry#getSingleton()` 方法

```java
public Object getSingleton(String beanName) {
    /** 参数true 设置标识允许早期依赖*/
    return getSingleton(beanName, true);
}


protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		//从Map中获取Bean，如果不为空直接返回，不在进行初始化工作
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 未被初始换 且 beanName 存在于正在被创建的单例Bean的池子中，进行初始化
        synchronized (this.singletonObjects) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                /**
                 * 当某些方法需要提前初始化的时候会调用
                 * addSingletonFactory方法将对应的ObjectFactory初始化策略
                 * 存储在 singletonFactories中
                 */
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    /** 调用预先设定的getObject()方法*/
                    singletonObject = singletonFactory.getObject();
                    /**
                     * 记录在缓存中
                     * earlySingletonObjects 与 singletonFactories 互斥
                     */
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```
#### `AbstractBeanFactory#getObjectForBeanInstance()` 方法
```java
protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

    // Don't let calling code try to dereference the factory if the bean isn't a factory.
    /**
     * 如果指定的name是工厂相关(以 & 为前缀)
     * 且beanInstance又不是 FactoryBean类型 则验证不通过
     */
    if (BeanFactoryUtils.isFactoryDereference(name)) {
        if (beanInstance instanceof NullBean) {
            return beanInstance;
        }
        if (!(beanInstance instanceof FactoryBean)) {
            throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
        }
    }

    // Now we have the bean instance, which may be a normal bean or a FactoryBean.
    // If it's a FactoryBean, we use it to create a bean instance, unless the
    // caller actually wants a reference to the factory.
    /**
     * 有了bean的实例，这个实例通常是 bean 或者是 FactoryBean
     * 如果是 FactoryBean 使用它创建实例，如果用户想要直接获取工厂实例
     * 而不是工厂的 getObject() 方法对应的实例，那么传入的name 应该加前缀 &
     */
    if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
        return beanInstance;
    }

    // 加载 FactoryBean
    Object object = null;
    if (mbd == null) {
        // 尝试从缓存中加载bean
        object = getCachedObjectForFactoryBean(beanName);
    }
    if (object == null) {
        // beanInstance 一定是 FactoryBean
        FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
        // Caches object obtained from FactoryBean if it is a singleton.
        if (mbd == null && containsBeanDefinition(beanName)) {
            /**
             * 将存储Xml配置文件的 GernericBeanDefinition 转换为 RootBeanDefinition
             * 如果指定BeanName是子 Bean 的话同时会合并父类的相关属性
             */
            mbd = getMergedLocalBeanDefinition(beanName);
        }
        // 是否用户定义的，而不是应用程序本身定义的
        boolean synthetic = (mbd != null && mbd.isSynthetic());
        object = getObjectFromFactoryBean(factory, beanName, !synthetic);
    }
    return object;
}
```

&ensp;&ensp;该方法是个高频使用的方法，无论是存缓存中获得bean 还是根据不同的 scope 策略加载bean。总之得到bean的实例之后要做的第一步就是调用这个方法来检测一下正确性，其实就是检测当前的
bean是否是FactoryBean类型的bean，如果是，那么需要调用该 bean 对应的 FactoryBean实例中的 getObject() 作为返回值。

&ensp;&ensp;该方法所做的工作主要有一下几点：

①：对 FactoryBean 正确性的验证

②：对非 FactoryBean 不做任何处理

③：对 bean 进行转换

④：从 Factory 中解析 bean 的工作委托给 `getObjectFromFactoryBean()`.

#### `FactoryBeanRegistrySupport#getObjectFromFactoryBean()` 方法
```java
protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
    // 如果是单例模式
    if (factory.isSingleton() && containsSingleton(beanName)) {
        synchronized (getSingletonMutex()) {
            Object object = this.factoryBeanObjectCache.get(beanName);
            if (object == null) {
                /** 通过FactoryBean获取对象*/
                object = doGetObjectFromFactoryBean(factory, beanName);
                // Only post-process and store if not put there already during getObject() call above
                // (e.g. because of circular reference processing triggered by custom getBean calls)
                Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
                if (alreadyThere != null) {
                    object = alreadyThere;
                }
                else {
                    if (shouldPostProcess) {
                        if (isSingletonCurrentlyInCreation(beanName)) {
                            // Temporarily return non-post-processed object, not storing it yet..
                            return object;
                        }
                        beforeSingletonCreation(beanName);
                        try {
                            /**
                             * 调用 ObjectFactory的后置处理器
                             * AbstractAutowireCapableBeanFactory#postProcessObjectFromFactoryBean()
                             * 尽可能保证所有的bean初始化之后都会调用注册的
                             * BeanPostProcessor 的 postProcessAfterInitialization 方法
                             */
                            object = postProcessObjectFromFactoryBean(object, beanName);
                        }
                        catch (Throwable ex) {
                            throw new BeanCreationException(beanName,
                                    "Post-processing of FactoryBean's singleton object failed", ex);
                        }
                        finally {
                            afterSingletonCreation(beanName);
                        }
                    }
                    if (containsSingleton(beanName)) {
                        this.factoryBeanObjectCache.put(beanName, object);
                    }
                }
            }
            return object;
        }
    }
    else {
        Object object = doGetObjectFromFactoryBean(factory, beanName);
        if (shouldPostProcess) {
            try {
                object = postProcessObjectFromFactoryBean(object, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
            }
        }
        return object;
    }
}


private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
        throws BeanCreationException {

    Object object;
    try {
        // 这里是权限验证，不重要
        if (System.getSecurityManager() != null) {
            AccessControlContext acc = getAccessControlContext();
            try {
                object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
            }
            catch (PrivilegedActionException pae) {
                throw pae.getException();
            }
        }
        else {
            // 直接调用 getObject()方法
            object = factory.getObject();
        }
    }
    catch (FactoryBeanNotInitializedException ex) {
        throw new BeanCurrentlyInCreationException(beanName, ex.toString());
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
    }

    // Do not accept a null value for a FactoryBean that's not fully
    // initialized yet: Many FactoryBeans just return null then.
    if (object == null) {
        if (isSingletonCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(
                    beanName, "FactoryBean which is currently in creation returned null from getObject");
        }
        object = new NullBean();
    }
    return object;
}
```
&ensp;&ensp;`doGetObjectFromFactoryBean`方法的主要就是：如果bean申明为 `FactoryBean`类型，则当提取bean时，提取的并不是
FactoryBean，而是FactoryBean中对应的 `getObject()`方法返回的bean。



