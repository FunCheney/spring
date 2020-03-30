## `doGetBean()`方法涉及的子过程解析

### 1.原型模式的依赖检查

`AbstractBeanFactory#isPrototypeCurrentlyInCreation(String
beanName)`方法

```java
protected boolean isPrototypeCurrentlyInCreation(String beanName) {
    Object curVal = this.prototypesCurrentlyInCreation.get();
    return (curVal != null &&
            (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
}
```
&ensp;&ensp;在`doGetBean()`方法中，如果当前正在创建的类是 prototype
类型的则抛出异常，异常信息 `Error creating bean with name `
【beanName】`Requested bean is currently in creation: Is there an
unresolvable circular reference?` 因为后面的代码涉及循环依赖的处理过程，由此可知
prototype 类型的bean
不做循环依赖的处理，只有在单例的情况下，spring才会尝试去解决循环依赖。





### 2.检测parentBeanFactory

### 3.BeanDefinition 的转换
