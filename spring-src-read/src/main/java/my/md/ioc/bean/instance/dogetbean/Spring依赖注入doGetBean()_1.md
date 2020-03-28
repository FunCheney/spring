## `doGetBean()`方法涉及的子过程解析

### 1.BeanName 的转换

**AbstractBeanFactory#transformedBeanName()**
```java
protected String transformedBeanName(String name) {
    return canonicalName(BeanFactoryUtils.transformedBeanName(name));
}
```
BeanFactoryUtils#transformedBeanName(name)
```java
public static String transformedBeanName(String name) {
    Assert.notNull(name, "'name' must not be null");
    if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
        return name;
    }
    return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
        do {
            beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
        }
        while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
        return beanName;
    });
}
```
#### 1.1`FactoryBean` 的使用
[见文章]

#### 1.2区分 `FactoryBean` 与 `BeanFactory`

- BeanFactory是Spring中IOC容器最核心的接口，遵循了IOC容器中所需的基本接口。例如我们很常见的：ApplicationContext，XmlBeanFactory 等等都使用了BeanFactory这个接口。
- FactoryBean是工厂类接口，当你只是想简单的去构造Bean，不希望实现原有大量的方法。它是一个Bean，不过这个Bean能够做为工厂去创建Bean，同时还能修饰对象的生成。
- FactoryBean比BeanFactory在生产Bean的时候灵活，还能修饰对象，带有工厂模式和装饰模式的意思在里面，不过它的存在还是以Bean的形式存在。

### 2.缓存中获取 getSingleton

DefaultSingletonBeanRegistry#getSingleton()
```java
public Object getSingleton(String beanName) {
    /** 参数true 设置标识允许早期依赖*/
    return getSingleton(beanName, true);
}
```

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 从Map中获取Bean，如果不为空直接返回，不在进行初始化工作
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 未被初始换 且 beanName 存在于正在被创建的单例Bean的池子中，进行初始化
        synchronized (this.singletonObjects) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                /*
                 * 当某些方法需要提前初始化的时候会调用
                 * addSingletonFactory方法将对应的ObjectFactory初始化策略
                 * 存储在 singletonFactories中
                 */
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    /* 调用预先设定的getObject()方法*/
                    singletonObject = singletonFactory.getObject();
                    /*
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
&ensp;&ensp;在getSingleton()方法中，这个方法因为涉及循环依赖的检测，所以涉及很多变量
的记录存取。首先，尝试从 singletonObjects 中获取实例；如果 获取不到 就从 earlySingletonObjects
中获取；如果 还获取不到，在尝试从singletonFactories里面获取beanName对应的ObjectFactory，
然后调用ObjectFactory的getObject() 方法来创建Bean，并放到 earlySingletonObjects 中去；
并且从 singletonFactories 里面remove掉这个 ObjectFactory，而对于后续所有的内存操作都只为
了循环依赖检测的时候使用，即 allowEarlyReference 为 true。

&ensp;&ensp;这里面涉及到几个Map和Set集合，一一做以解释：

```
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

private final Set<String> registeredSingletons = new LinkedHashSet<>(256);
```

①：singletonObjects 用于保存BeanName 和 创建 Bean 实例之间的的关系，beanName --> beanInstance

②：singletonFactories 用户保存 BeanName 和创建 bean 实例之间的关系，beanName --> ObjectFactory

③：earlySingletonObject 也是用来保存 beanName 和创建 Bean 实例之间的关系，与①的不同之处在于，
当一个单例bean被放到这里面之后，那么当bean还在创建过程中，就可以使用getBean() 方法获取到了，其目的就是用来检测循环引用。

④：registeredSingletons 用来保存当前已经注册的Bean


### 3.对于 `sharedInstance`的判断
&ensp;&ensp;在前面介绍的`doGetBean()`方法中有这样的一个代码片段 `(sharedInstance != null && args == null)`，  
在通过上一个上一步骤 `getSingleton()`后，对获取的对象的处理。 如果满足上述条件的判断，会进入  
到方法`getObjectForBeanInstance()`中。

**AbstractBeanFactory#getObjectForBeanInstance()方法**

```java
protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		/*
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

		/*
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
        // 通过FactoryBean获取对象
        object = getObjectFromFactoryBean(factory, beanName, !synthetic);
    }
    return object;
}
```
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

&ensp;&ensp;在该方法中涉及到连个字方法`doGetObjectFromFactoryBean()`以及`postProcessObjectFromFactoryBean()`,逐个
分析一下这连个方法里面的实现逻辑。

#### 3.1 `doGetObjectFromFactoryBean()`
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

#### 3.2 `postProcessObjectFromFactoryBean()`
**AbstractAutowireCapableBeanFactory#postProcessObjectFromFactoryBean()**
```java
protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
    return applyBeanPostProcessorsAfterInitialization(object, beanName);
}
```
```java
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
        throws BeansException {

    // existingBean 为原对象
    Object result = existingBean;
    // 获取所有的 BeanPostProcessor 循环处理
    for (BeanPostProcessor processor : getBeanPostProcessors()) {
        Object current = processor.postProcessAfterInitialization(result, beanName);
        if (current == null) {
            return result;
        }
        result = current;
    }
    return result;
}
```
****


[见文章]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/Spring%E5%AE%B9%E5%99%A8%E4%B9%8BFactoryBean%E7%9A%84%E4%BD%BF%E7%94%A8.md


