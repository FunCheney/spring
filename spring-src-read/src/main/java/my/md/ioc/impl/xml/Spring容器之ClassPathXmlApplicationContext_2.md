## ClassPathXmlApplicationContext 的源码阅读

#### `DefaultSingletonBeanRegistry#getSingleton()` 方法
&ensp;&ensp;之前讲了从缓存中获取单例的过程，那么，如果缓存中不存在已经加载的单例bean就需要
从头开始 bean 的加载过程了，而 Spring 中使用了 `getSingleton()`的重载方法实现bean的加载过程
```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    Assert.notNull(beanName, "Bean name must not be null");
    /** 全局变量 需要同步*/
    synchronized (this.singletonObjects) {
        /**
         * 首先检查对应的Bean是否已经加载过，
         * singleton 就是复用以前创建的Bean，这一步是必须的
         */
        Object singletonObject = this.singletonObjects.get(beanName);
        // 如果为空 才可以进行 singleton 的初始化
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
                afterSingletonCreation(beanName);
            }
            if (newSingleton) {
                // 加入缓存
                addSingleton(beanName, singletonObject);
            }
        }
        return singletonObject;
    }
}
```