## `doGetBean()`方法涉及的子过程解析

### 1.BeanName 的转换

AbstractBeanFactory#transformedBeanName()
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

#### 1.2区分 `FactoryBean` 与 `BeanFactory`



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

