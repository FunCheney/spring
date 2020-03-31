## Spring依赖注入之createBean()
### `singleton` Bean的实例化

`doGetBean()`中的代码片段：

```java
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
```
&ensp;&ensp;上述代码中设计到两个步骤，一个是`getSingleton()`的过程，一个是`createBean（）`的过程。在
`doGetBean()`中，前面有一个通过`beanName`，`etSingleton()`的方法。这两个方法，都是通过方法的重载的方式，
获取容器中的单例`Bean`。

**DefaultSingletonBeanRegistry#getSingleton(String, ObjectFactory<?>)
方法的实现**

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(beanName, "Bean name must not be null");
    /* 全局变量 需要同步*/
    synchronized (this.singletonObjects) {
        /*
         * 首先检查对应的Bean是否已经加载过，
         * singleton 就是复用以前创建的Bean，这一步是必须的
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
        return singletonObject;
    }
}
```
&ensp;&ensp;其中，`beforeSingletonCreation()`中记录加载状态，通过`this.singletonsCurrentlyInCreation.add(beanName)`
将当前正在创建的 `bean` 记录在缓存中，这样便可以对循环依赖进行检测。

```java
protected void beforeSingletonCreation(String beanName) {
    if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
        throw new BeanCurrentlyInCreationException(beanName);
    }
} 
```

&ensp;&ensp;其中`afterSingletonCreation()`中完成当 `bean`
加载结束后需要移除缓存中对Bean加载状态的记录。

```java
protected void afterSingletonCreation(String beanName) {
    if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
        throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
    }
}
```

其中，`addSingleton()`方法就是将对象的对象放置到Map当中。

```java
protected void addSingleton(String beanName, Object singletonObject) {
    synchronized (this.singletonObjects) {
        this.singletonObjects.put(beanName, singletonObject);
        this.singletonFactories.remove(beanName);
        this.earlySingletonObjects.remove(beanName);
        this.registeredSingletons.add(beanName);
    }
}
```

&ensp;&ensp;上述代码中使用了回调方法，使得程序在单例创建前后做一些准备及处理操作，真正获取单例beande方法
其实并不是在该方法中实现的，其实现逻辑是调用 `ObjectFactory`
的`createBean()`方法来实现的。这里所做的准备操作如下：

①：检查缓存是否已经加载过

②：如果没有加载，则记录beanName的正在加载状态

③：加载单例前记录加载状态

④：通过调用参数传入的ObjectFactory的个体Object方法实例化bean

⑤：加载单例后初始方法的调用，当bean加载结束后需要移除缓存中对该bean的正在加载状态的记录

⑥：将结果记录至缓存并删除加载 bean 过程中所记录的各种辅助状态

⑦：返回处理结果





