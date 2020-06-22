## Spring Bean 的实例化_01
&ensp;&ensp;在Spring中Bean的实例化是一个及其复杂的过程，从这篇文章开始，我就要学习容器初始化 `refresh()`方法中的 `inishBeanFactoryInitialization(beanFactory);`
### finishBeanFactoryInitialization() 方法代码实现
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
&ensp;&ensp;在上述的方法中，最重要的就是 `preInstantiateSingletons()`。这里我自己的理解就是预实里化容器之非懒加载（`Lazy-init`）的对象。

### preInstantiateSingletons() 
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
&ensp;&ensp;从上述方法中可以看出，循环容器初始时添加到容器中的所有的 `BeanName`，然后根据 `BeanName` 获取到对应的 `BeanDefinition`。
通过 `BeanDefinition`中的属性来判断是否要触发相应的 `getBean()` 过程。
&ensp;&ensp;在 `getBean()` 之前，我首先要做的就是试着画一下对应的粗略流程图，后面的文章，将会一一的张开流程图里面的子流程。

doGetBean_1.jpg

&ensp;&ensp;看完了整个流程图，下面就要开始第一个方法 `getSingleton(String beanName)` 的处理逻辑

### getSingleton(String beanName)
```java
public Object getSingleton(String beanName) {
    /** 参数true 设置标识允许早期依赖*/
    return getSingleton(beanName, true);
}
```
&ensp;&ensp;调用重载的方法 `getSingleton(String beanName, boolean allowEarlyReference)` 来进行真正的逻辑处理，在Spring 中默认是允许
早期依赖的，也即 `allowEarlyReference` 为 `true`。

```java
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
在创建Bean的时候，首先，先到 `singletonObjects` 中获取，这对象是一个 `Map`，用来存放已经创建好的对象。里面的对象可以直接使用。
&ensp;&ensp;通过下面的图片来介绍上述 `getSingleton()` 的过程：

getSingleton_1.jpg

&ensp;&ensp;对于这个 `getSingleton()` 方法而言，在容器初始化的时候，`singletonObjects` 中必然没有对象，范湖的结果则必然为null。

### 初识 三个Map
&ensp;&ensp;在上述的三个方法中，我们将会看到有用到如下的三个 Map；
`private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);`、
`private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);`、
`private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);`
这三个Map，在之后的依赖注入的处理中起着重要的作用。其中 `singletonObjects` 用于存放完全初始化好的bean；`singletonFactories` 用于存放
bean工厂对象； `earlySingletonObjects` 存放原始的bean对象。在Spring中定义了三个Map，缓存Spring容器中实力化的 `Bean`，并解决了注入过程
中循环依赖的问题。

### 标记Bean是已创建的 
```java
protected void markBeanAsCreated(String beanName) {
    // 没有创建
    if (!this.alreadyCreated.contains(beanName)) {
        // 通过 synchronized 保证只有一个线程创建
        synchronized (this.mergedBeanDefinitions) {
            // 再次检查 没有创建
            if (!this.alreadyCreated.contains(beanName)) {
                // 从 mergedBeanDefinitions 中删除 beanName，
                // 并在下次访问时重新创建它
                clearMergedBeanDefinition(beanName);
                // 添加到已创建bean 集合中
                this.alreadyCreated.add(beanName);
            }
        }
    }
}
```
&ensp;ensp;容器初始化之处，这里我们自己的 `Bean`对应的 `beanName` 在 `alreadyCreated` 中必然是不存在的。因此，Spring在这里，就会将当前
的 `BeanName` 添加到 `alreadyCreated` 的集合中，用来标记当前的 `Bean` 是已创建的。这里为了保证添加一次，还采用了双重检测的方式。
