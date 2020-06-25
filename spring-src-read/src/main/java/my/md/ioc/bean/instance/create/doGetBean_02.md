## Spring Bean 的实例化_02
&ensp;&ensp;在上篇文章中提到 `Object sharedInstance = getSingleton(beanName);` 这一行代码，在容器初始化的时候返回的对象为`null`。
但是，今天这篇文章中，我要先来处理 `sharedInstance 不为null`的情况，看看Spring做了什么样的处理。对应下述流程图中的左侧部分：
doGetBean_1.jpg

### getObjectForBeanInstance
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
             * 将存储Xml配置文件的 GenericBeanDefinition 转换为 RootBeanDefinition
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
&ensp;&ensp;该方法是个高频使用的方法，无论是存缓存中获得`bean` 还是根据不同的 `scope` 策略加载`bean`。总之得到`bean`的实例之后要做的第一步就是
调用这个方法来检测一下正确性，其实就是检测当前的`bean`是否是`FactoryBean`类型的`bean`，如果是，那么需要调用该`bean`对应的`FactoryBean`实例中的`getObject()`
 作为返回值。

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
```
### 通过FactoryBean 获取对象
```java
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
&ensp;&ensp;上述代码中最重要的一行就是 ` object = factory.getObject();`。可以看到，这里的 `factory` 就是我们传入的 `beanInstance`。
在 `getObjectForBeanInstance` 中做了转换`FactoryBean<?> factory = (FactoryBean<?>) beanInstance;` 。 回到最初的起点，`getBean(beanName)`
通过 `beanName` 获取对象，最终得到的是调用 `FactoryBean` 的 `getObject()` 方法返回的对象。

