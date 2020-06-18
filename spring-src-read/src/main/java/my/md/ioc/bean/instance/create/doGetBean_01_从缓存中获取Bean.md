### 从缓存中获取Bean
```java
public Object getSingleton(String beanName) {
    /** 参数true 设置标识允许早期依赖*/
    return getSingleton(beanName, true);
}
```
&ensp;&ensp;在创建Bean的时候，首先，先到 `singletonObjects` 中获取，这对象是一个 `Map`，用来存放已经创建好的对象。里面的对象可以直接使用。
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
&ensp;&ensp;通过下面的图片来介绍上述 `getSingleton()` 的过程：

&ensp;&ensp;对于这个 `getSingleton()` 方法而言，在容器初始化的时候，`singletonObjects` 中没有对象， 
