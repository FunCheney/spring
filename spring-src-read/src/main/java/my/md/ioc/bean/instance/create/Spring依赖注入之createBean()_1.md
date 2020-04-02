## Spring依赖注入之createBean()
### `prototype` Bean的实例化
```java
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
```
&ensp;&ensp;`beforePrototypeCreation` 把 `beanName` 加入到 `ThreadLocal`
中，可以用于避免线程重复创建该`bean`。代码实现如下
```java
protected void beforePrototypeCreation(String beanName) {

    Object curVal = this.prototypesCurrentlyInCreation.get();
    if (curVal == null) {
        this.prototypesCurrentlyInCreation.set(beanName);
    }
    else if (curVal instanceof String) {
        Set<String> beanNameSet = new HashSet<>(2);
        beanNameSet.add((String) curVal);
        beanNameSet.add(beanName);
        this.prototypesCurrentlyInCreation.set(beanNameSet);
    }
    else {
        Set<String> beanNameSet = (Set<String>) curVal;
        beanNameSet.add(beanName);
    }
}
```
&ensp;&ensp;然后就是`createBean()`的过程，这个过程，与单例`Bean`的创建一样，后面会有详细的介绍。

&ensp;&ensp;在然后就是原型`Bean`创建之后的回调，创建之后，将
`beanName`从创建的线程中移除。

```java
protected void afterPrototypeCreation(String beanName) {
    Object curVal = this.prototypesCurrentlyInCreation.get();
    if (curVal instanceof String) {
        this.prototypesCurrentlyInCreation.remove();
    }
    else if (curVal instanceof Set) {
        Set<String> beanNameSet = (Set<String>) curVal;
        beanNameSet.remove(beanName);
        if (beanNameSet.isEmpty()) {
            this.prototypesCurrentlyInCreation.remove();
        }
    }
}
```
&ensp;&ensp;最后就是 `getObjectForBeanInstance()` 方法，通过
`BeanInstance`获取对象。

