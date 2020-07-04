### 万众期待的 doCreateBean

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

    // BeanWrapper 是用来持有创建出来的Bean对象
    BeanWrapper instanceWrapper = null;
    if (mbd.isSingleton()) {
        // 如果是singleton，先清除缓存中同名的Bean
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    if (instanceWrapper == null) {
        /*
         * 通过createBeanInstance 来完成Bean所包含的java对象的创建。
         * 对象的生成有很多种不同的方式，可以通过工厂方法生成，
         * 也可以通过容器的autowire特性生成，这些生成方式都是由BeanDefinition来指定的
         */
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    /* getWrappedInstance() 获得原生对象*/
    final Object bean = instanceWrapper.getWrappedInstance();
    Class<?> beanType = instanceWrapper.getWrappedClass();
    if (beanType != NullBean.class) {
        mbd.resolvedTargetType = beanType;
    }

    // Allow post-processors to modify the merged bean definition.
    synchronized (mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            try {
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Post-processing of merged bean definition failed", ex);
            }
            mbd.postProcessed = true;
        }
    }

    /*
     * 是否需要提前曝光：单例 & 允许循环依赖& 当前的bean正在创建中，检测循环依赖
     */
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
            isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        if (logger.isTraceEnabled()) {
            logger.trace("Eagerly caching bean '" + beanName +
                    "' to allow for resolving potential circular references");
        }
        /*
         * setter 方法注入的Bean，通过提前暴露一个单例工厂方法
         * 从而能够使其他Bean引用到该Bean，注意通过setter方法注入的
         * Bean 必须是单例的才会到这里来。
         * 对 Bean 再一次依赖引用，主要应用 SmartInstantiationAwareBeanPostProcessor
         * 其中 AOP 就是在这里将advice动态织入bean中，若没有则直接返回，不做任何处理
         *
         * 在Spring中解决循环依赖的方法：
         *   在 B 中创建依赖 A 时通过 ObjectFactory 提供的实例化方法来中断 A 中的属性填充，
         *   使 B 中持有的 A 仅仅是刚初始化并没有填充任何属性的 A，初始化 A 的步骤是在最开始创建A的时候进行的，
         *   但是 因为 A 与 B 中的 A 所表示的属性地址是一样的，所以在A中创建好的属性填充自然可以通过B中的A获取，
         *   这样就解决了循环依赖。
         */
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // Initialize the bean instance.
    /*
     * 将原生对象复制一份 到 exposedObject，这个exposedObject在初始化完成处理之后
     * 会作为依赖注入完成后的Bean
     */
    Object exposedObject = bean;
    try {
        /* Bean的依赖关系处理过程*/
        populateBean(beanName, mbd, instanceWrapper);
        /* 将原生对象变成代理对象*/
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    catch (Throwable ex) {
        if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
            throw (BeanCreationException) ex;
        }
        else {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
        }
    }

    if (earlySingletonExposure) {
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                String[] dependentBeans = getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                for (String dependentBean : dependentBeans) {
                    // 检测循环依赖
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }
                /*
                 * 因为Bean创建后其所依赖的Bean一定是已经创建的，
                 * actualDependentBeans 不为空则表示当前bean创建后其依赖的Bean却没有全部创建完，
                 * 也就是说存在循环依赖
                 */
                if (!actualDependentBeans.isEmpty()) {
                    throw new BeanCurrentlyInCreationException(beanName,
                            "Bean with name '" + beanName + "' has been injected into other beans [" +
                            StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                            "] in its raw version as part of a circular reference, but has eventually been " +
                            "wrapped. This means that said other beans do not use the final version of the " +
                            "bean. This is often the result of over-eager type matching - consider using " +
                            "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                }
            }
        }
    }

    // Register bean as disposable.
    try {
        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }

    return exposedObject;
}
```
&ensp;&ensp;简单归纳上述方法都完成了什么事：

 * ①：如果是单例首先需要清除缓存
 * ②：实例化 bean，将 BeanDefinition 转化为 BeanWrapper
 * ③：MergedBeanDefinitionPostProcessor 的应用
 * ④：依赖处理
 * ⑤：属性填充
 * ⑥：循环依赖处理
 * ⑦：注册 DisposableBean
 * ⑧：完成创建并返回
 
&ensp;&ensp;`isSingletonCurrentlyInCreation(beanName)`在这里返回的时候为`true`。还记得在什么地方，将当前正在创建的`beanName`放入到 
 `singletonsCurrentlyInCreation`这个`Set` 集合中吗？
 
 **答案就是：** `getSingleton(String beanName, ObjectFactory<?> singletonFactory)`方法中的 `beforeSingletonCreation(beanName)`方法中
 addsingletonsCurrentlyInCreation.jpg
 
 &ensp;&ensp;在这里需要指出 `addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));` 这一行代码在Spring中解决
 循环依赖起着非常重要的作用。其原理就是，提前暴露 `ObjectFactory` 将其添加到`singletonFactories`中去，然后通过`getObject()`返回对象。
 
### 创建bean
&ensp;&ensp;在今天这篇文章中，还将介绍创建 `createBeanInstance()` 返回 `BeanWrapper` 对象的入口，应为在Spring中创建对象的方式做了区分，
分为通过: 使用工厂方法对Bean进行实例化、通过构造方法自动装配的方式构造Bean对象、通过默认的无参构造方法进行实例化。每一种方式都有对应的方法进行处理。 
```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // Make sure bean class is actually resolved at this point.
    /** 确认需要实例化的bean */
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    /**
     * 检查一个类的访问权限，spring默认情况下，对于非public的类是允许访问的
     */
    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }

    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }

    /** 使用工厂方法对Bean进行实例化*/
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    /**
     * 多次构建同一个Bean时，可以使用shortcut
     */
    boolean resolved = false;
    boolean autowireNecessary = false;
    if (args == null) {
        synchronized (mbd.constructorArgumentLock) {
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                resolved = true;
                // 如果已经解析了构造方法的参数，就必须通过一个带参数的构造方法来实例
                autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }
    }
    if (resolved) {
        if (autowireNecessary) {
            // 通过构造方法自动装配的方式构造Bean对象
            return autowireConstructor(beanName, mbd, null, null);
        }
        else {
            // 通过默认的无参构造方法进行实例化
            return instantiateBean(beanName, mbd);
        }
    }

    // Candidate constructors for autowiring?
    /**
     * 使用构造函数进行实例化
     * 由后置处理器决定返回那些构造方法
     * 当有多个构造器的时候，Spring 认为是没有通过构造器初始化的
     */
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
            mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // Preferred constructors for default construction?

    ctors = mbd.getPreferredConstructors();
    if (ctors != null) {
        return autowireConstructor(beanName, mbd, ctors, null);
    }

    // No special handling: simply use no-arg constructor.
    /** 使用默认的构造函数*/
    return instantiateBean(beanName, mbd);
}
```
&ensp;&ensp;
### BeanWrapper 对象
&ensp;&ensp;这个方法返回主要是用来返回 `BeanWrapper` 对象。我自己对 `BeanWrapper` 的理解就是 `Bean` 的包装。Spring提供的一个用来操
作javaBean属性的工具，使用它可以直接修改一个对象的属性。`BeanWrapper`本身是一个接口，它提供了一整套处理Bean的方法，代码如下：
```java
public interface BeanWrapper extends ConfigurablePropertyAccessor {

	/**
	 * 为数组和集合自动增长指定一个限制。在普通的BeanWrapper上默认是无限的。
	 */
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * 返回数组和集合自动增长的限制。
	 */
	int getAutoGrowCollectionLimit();

	/**
	 * 返回对象包装的bean实例
	 */
	Object getWrappedInstance();

	/**
	 * 返回被包装的Bean对象的类型。
	 */
	Class<?> getWrappedClass();

	/**
	 * 获取包装对象的PropertyDescriptors
	 */
	PropertyDescriptor[] getPropertyDescriptors();

	/**
	 * 获取包装对象的特定属性的属性描述符。
	 */
	PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}

```
&ensp;&ensp;这个 `BeanWrapper` 在Spring中是对Bean实力化来说一个非常重要的对象，在进行依赖关系处理的时候会使用到该对象，后面的文章中会讲到。