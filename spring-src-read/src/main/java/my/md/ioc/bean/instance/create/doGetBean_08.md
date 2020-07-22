## 依赖关系的处理
&ensp;&ensp;上一篇文章中，通过 `createBeanInstance()` 方法，最终得到了 `BeanWrapper` 对象。再得到这个对象之后，在Spring中，对于依赖
关系的处理，是通过 `BeanWrapper` 来完成的。

### 1.自动装配与@Autowired
&ensp;&ensp;这里首先做一个区分，因为在之前的很长一段时间内，我都错误的以为 `@Autowired` 就是自动装配。这也就引发了我一直错误的任务Spring的自动
装配首先是 `byType` 然后是 `byName` 的。通过这段时间对于源码的阅读，我才意识到这个错误。

&ensp;&ensp;当涉及到自动装配Bean的依赖关系时，Spring提供了4种自动装配策略。

```java
public interface AutowireCapableBeanFactory{
 
 //无需自动装配
 int AUTOWIRE_NO = 0;
 
 //按名称自动装配bean属性
 int AUTOWIRE_BY_NAME = 1;
 
 //按类型自动装配bean属性
 int AUTOWIRE_BY_TYPE = 2;
 
 //按构造器自动装配
 int AUTOWIRE_CONSTRUCTOR = 3;
 
 //过时方法，Spring3.0之后不再支持
 @Deprecated
 int AUTOWIRE_AUTODETECT = 4;
 ...
}
```

#### 1.1 自动装配
&ensp;&ensp;在 `xml` 中定义 `Bean`的时候，可以通过如下的方式指定自动装配的类型。
```xml
<bean id="demoServiceOne" class="DemoServiceOne" autowire="byName"/>
```
```xml
<bean id="userService" class="UserService" autowire="byType"/>
```
```xml
<bean id="user" class="User" autowire="constructor"></bean>
```

如果使用了根据类型来自动装配，那么在IOC容器中只能有一个这样的类型，否则就会报错！
#### 1.2 使用注解来实现自动装配
&ensp;&ensp;`@Autowired` 注解，它可以对类成员变量、方法及构造函数进行标注，完成自动装配的工作。Spring是通过 `@Autowired` 来实现自动装配的。
当然，Spring还支持其他的方式来实现自动装配，如：**JSR-330的@Inject注解**、**JSR-250的@Resource注解**。

&ensp;&ensp;通过注解的方式来自动装配 `Bean` 的属性，它允许更细粒度的自动装配，我们可以选择性的标注某一个属性来对其应用自动装配。

### 2.依赖注入
&ensp;&ensp;在这篇文章中，我将详细的分析，在一个对象中通过 `@Autowired`注入或 `@Resource` 注入属性的处理过程。

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
    if (bw == null) {
        if (mbd.hasPropertyValues()) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
        }
        else {
            // Skip property population phase for null instance.
            // 没有任何属性需要填充
            return;
        }
    }

    // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
    // state of the bean before properties are set. This can be used, for example,
    // to support styles of field injection.
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                /**
                 * InstantiationAwareBeanPostProcessor 的 postProcessAfterInstantiation() 方法的应用
                 * 可以控制程序是否进行属性填充
                 */
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    return;
                }
            }
        }
    }

    /**
     * 取得BeanDefinition 中设置的 Property值，
     * 这些property来自对BeanDefinition的解析，
     * 具体的过程可以参看对载入个解析BeanDefinition的分析
     * 这里是Spring 内部设置的属性值，一般不会设置
     */
    PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

    /**
     * 处理自动装配，xml的方式可能会有配置自动装配类型的情况 
     */
    int resolvedAutowireMode = mbd.getResolvedAutowireMode();
    // Spring 默认 既不是 byType 也不是 byName, 默认是null
    if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
        // Add property values based on autowire by name if applicable.
        /**
         * byName 处理
         * 通过反射从当前Bean中得到需要注入的属性名，
         * 然后使用这个属性名想容器申请与之同名的Bean，
         * 这样实际有处发了另一个Bean生成和依赖注入的过程
         */
        if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }
        // Add property values based on autowire by type if applicable.
        // byType 处理
        if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }
        pvs = newPvs;
    }

    // 后置处理器已经初始化
    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
    // 需要检查依赖
    boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

    PropertyDescriptor[] filteredPds = null;
    if (hasInstAwareBpps) {
        if (pvs == null) {
            // 与构造方法的处理一样，对象有但对象里面的属性没有
            pvs = mbd.getPropertyValues();
        }
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                // 通过后置处理器来完成处理
                PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
                if (pvsToUse == null) {
                    if (filteredPds == null) {
                        filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                    }
                    pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                    if (pvsToUse == null) {
                        return;
                    }
                }
                pvs = pvsToUse;
            }
        }
    }
    if (needsDepCheck) {
        if (filteredPds == null) {
            filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
        }
        checkDependencies(beanName, mbd, filteredPds, pvs);
    }

    if (pvs != null) {
        // 对属性进行注入
        applyPropertyValues(beanName, mbd, bw, pvs);
    }
}
```
