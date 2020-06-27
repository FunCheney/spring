## Spring Bean 的实例化_03
&ensp;&ensp; 今天要学习的是 `Bean` 实力华的第三篇文章。第一篇文章中，从入口开始，找到了在Spring IoC 容器初始化的时候，如何实例化 `bean`。
上一篇文章，从 `getSingleton(beanName)` 返回的对象若不为空开始分析，Spring 是如何通过 `beanInstance` 来完成对象的实例化的。今天这篇文章，
将从另一个分支开始，即 `getSingleton(beanName)` 返回的对象为 `null`时的处理。这一过程，也是在容器初始化之处，大多数`getBean()` 方法走的过程。

### getSingleton(String beanName, ObjectFactory<?> singletonFactory)
&ensp;&ensp;这个方法是这一过程的开始，也是重点方法，代码实现如下：
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
对其进行分析，这篇文章中暂时不做处理。对应这里的 `singletonFactory.getObject()`。 

### 实例化之后的处理
```java
protected void afterSingletonCreation(String beanName) {
    if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
        throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
    }
}
```
&ensp;&ensp;当 `singletonFactory.getObject();` 结束时候，Spring 也对其做了处理，与`beforeSingletonCreation()` 的处理过程相反。

### 总结
①：容器实例化 `Bean` 的入口是 `getSingleton(String beanName, ObjectFactory<?> singletonFactory
)` 方法中的 `singletonFactory.getObject()`。 

②：实例化之前会向 正在创建的 `BeanName` 的 `Set` 集合`singletonsCurrentlyInCreation`中添加当前`Bean`的名称进去。并在创建完成之后删除。

③：`getSingleton()` 方法是 Spring 中重要的方法，通过不同的重写方法，来完成不同的逻辑。截止到目前，关于 `Bean` 实例化的文章都离不开这个方法。
