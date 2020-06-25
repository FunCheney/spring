### Spring Bean 的实例化_03
&ensp;&ensp;
```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(beanName, "Bean name must not be null");
    /** 全局变量 需要同步*/
    synchronized (this.singletonObjects) {
        /**
         * 首先检查对应的Bean是否已经加载过，
         * singleton 就是复用以前创建的Bean，这一步是必须的,
         * 在容器创建初始化之处，这里拿到的一定是 null
         */
        Object singletonObject = this.singletonObjects.get(beanName);
        /** 如果为空 才可以进行 singleton 的初始化 */
        if (singletonObject == null) {
            if (this.singletonsCurrentlyInDestruction) {
                throw new BeanCreationNotAllowedException(beanName,
                        "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                        "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
            }
            /**
             * 把beanName添加到 singletonsCurrentlyInCreation 的set集合中
             * 表示beanName正在创建中
             * 加载单例前记录加载状态
             */
            beforeSingletonCreation(beanName);
            boolean newSingleton = false;
            boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
            if (recordSuppressedExceptions) {
                this.suppressedExceptions = new LinkedHashSet<>();
            }
            try {
                /** 初始化 bean */
                singletonObject = singletonFactory.getObject();
                newSingleton = true;
            }
            catch (IllegalStateException ex) {
                // Has the singleton object implicitly appeared in the meantime ->
                // if yes, proceed with it since the exception indicates that state.
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    throw ex;
                }
            }
            catch (BeanCreationException ex) {
                if (recordSuppressedExceptions) {
                    for (Exception suppressedException : this.suppressedExceptions) {
                        ex.addRelatedCause(suppressedException);
                    }
                }
                throw ex;
            }
            finally {
                if (recordSuppressedExceptions) {
                    this.suppressedExceptions = null;
                }
                /** 加载单例后初始方法的调用*/
                afterSingletonCreation(beanName);
            }
            if (newSingleton) {
                /**
                 * 加入缓存
                 * 将结果记录至缓存并删除加载 bean 过程中所记录的各种辅助状态
                 */
                addSingleton(beanName, singletonObject);
            }
        }
        // 返回处理结果
        return singletonObject;
    }
}
```
&ensp;&ensp;这个 `getSingleton()` 方法要区别上一篇文章找那个提到的 `getSingleton()` 方法。这两个方法是重载方法，对应的实现逻辑也不相同。
这里的这个方法的处理逻辑与上一篇文章中提到的 `getSingleton()` 方法为重载的方法，但实现的逻辑不尽相同。通过如下的流程图来看：

doGetBean-getSingleton_2.jpg


### 实例化之前的处理与判断
```java
protected void beforeSingletonCreation(String beanName) {
    if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
        throw new BeanCurrentlyInCreationException(beanName);
    }
}
```
&ensp;&ensp;在上述方法中调用 `singletonFactory.getObject();` 之前，首先会调用`beforeSingletonCreation()` 方法对当前创建的 `Bean` 进行检查。 
判断当前的 `Bean`名称不在当前排除创建检查的 `Set` 集合中。并且，该 `beanName` 同时也不再用来记录正在创建的的`BeanName`的集合中。这里通过向 `Set`
结合中添加元素的方式来判断。
beforeSingletonCreation.jpg
### Bean 的实例化
&ensp;&ensp;对应的 `Bean的实例化` 在图`doGetBean-getSingleton_2.jpg` 中有做相应的标注，对应的该过程同样也是一个极其复杂的过程。在或许的文章中，会逐个
对其进行分析，这篇文章中暂时不做处理。 

### 实例化之后的处理
&ensp;&ensp;从代码的实现上来看，在创建完成对应的 `Bean` 之后，会对其进行依赖检查。这里的这个 `Bean` 是通过 `getSingleton()` 方法得到的 `sharedInstance` 对象
后通过 `getObjectForBeanInstance()` 得到的对象。

afterGetBean.jpg

### 万众期待的 doGetBean() 方法
&ensp;&ensp;Spring的方法学习了这么久，对于Spring的行事风格也有了一定的了解，看到 `doXXX` 的方法，要足够重视。这里的 `Bean`的实例化也不例外。上述的几个方法都是在
`doGetBean()` 方法中的子调用流程。下面我将试图从源码和`doGetBean()`时序图来进行分析：
#### 源码实现及分析
```java
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
        @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

    // 转换beanName, FactoryBean 的name会已& 开头
    final String beanName = transformedBeanName(name);
    Object bean;

    /*
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
        /*
         * getObjectForBeanInstance() 完成的是FactoryBean的相关处理，以取得FactoryBean的生产结果，
         * BeanFactory 和 FactoryBean的区别 查看之前的解释
         * 存在BeanFactory的情况并不是直接返回实例本身，而是返回指定方法返回的实例
         */
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }

    else {
        /*
         * 当前正在创建的类是否是prototype，如果是就报错
         * 只有在单例情况下才会尝试解决循环依赖
         */
        if (isPrototypeCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }

        /*
         * 判断IOC容器中的 BeanDefinition 是否存在，检查是否能在当前的BeanFactory中取得需要的Bean，
         * 如果当前的 BeanFactory 中取不到，则到双亲的 BeanFactory 中去取；
         * 如果当前的双亲工厂中取不到，就顺着双亲BeanFactory 链一直向上查找;
         * parentBeanFactory 如果不对 Spring 做处理，这里一般得到的 BeanFactory 都为空
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
&ensp;&ensp;这个方法的内容，本片文章的要介绍的，已经在本文的前半部分做了介绍。这里只是，将整个方法拿出来，从整个框架上面看一下这个方法做了些什么事，大致总结为如下的
几个方面：

①：转换对应的BeanName

②：尝试存缓存中加载单例Bean

③：bean的实例化

④：原型模式的依赖检查

⑤：检测parentBeanFactory

⑥：将存储XML配置文件的GenericBeanDefinition 转换为 RootBeanDefinition

#### 时序图




