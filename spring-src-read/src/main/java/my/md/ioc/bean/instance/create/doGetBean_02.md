### Spring Bean 的实例化_01
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

### 实例化之前的处理与判断

### Bean 的实例化

### 实例化之后的处理

