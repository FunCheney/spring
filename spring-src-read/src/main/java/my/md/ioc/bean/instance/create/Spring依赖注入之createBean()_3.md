## Spring依赖注入之createBean()
### 准备创建 bean
&ensp;&ensp;在Spring中，真正做事的方法都是以 `doXXX`来完成的。因此，将
`AbstractAutowireCapableBeanFactory` 中的 `createBean(String beanName,
RootBeanDefinition mbd, @Nullable Object[] args)`
方法，称为准备创建bean。代码如下：

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
        throws BeanCreationException {

    if (logger.isTraceEnabled()) {
        logger.trace("Creating instance of bean '" + beanName + "'");
    }
    RootBeanDefinition mbdToUse = mbd;

    // Make sure bean class is actually resolved at this point, and
    // clone the bean definition in case of a dynamically resolved Class
    // which cannot be stored in the shared merged bean definition.
    /** 判断需要创建的Bean是否可以实例化，这个类是否可以通过类装载器载入 */
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    // Prepare method overrides.
    try {
        /**
         * 处理lockup-method 和 replace-method配置，spring将这两个方法统称为 method overrides
         */
        mbdToUse.prepareMethodOverrides();
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                beanName, "Validation of method overrides failed", ex);
    }

    try {
        // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
        // 如果Bean配置了PostProcessor，这里返回的是一个Proxy
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
        if (bean != null) {
            // 在配置了PostProcessor的处理器中 改变了Bean，直接返回
            return bean;
        }
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                "BeanPostProcessor before instantiation of bean failed", ex);
    }

    try {
        /**
         * 创建Bean的实例，若bean的配置信息中配置了 lookup-method 和 replace-method
         */
        Object beanInstance = doCreateBean(beanName, mbdToUse, args);
        if (logger.isTraceEnabled()) {
            logger.trace("Finished creating instance of bean '" + beanName + "'");
        }
        return beanInstance;
    }
    catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
        // A previously detected exception with proper bean creation context already,
        // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
    }
}
```
该函数完成的具体步骤即功能：

①：根据设置的 class 属性或者根据 className 来解析 Class;

②：对 override 属性进行标记处理及验证;

③：应用初始换前的后置处理器，解析指定 bean 是否存在初始化前的短路操作;

④：创建bean。

#### 处理 `override` 属性

```java
public void prepareMethodOverrides() throws BeanDefinitionValidationException {
    // Check that lookup methods exist and determine their overloaded status.
    if (hasMethodOverrides()) {
        getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
    }
}
```

```java
protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
    /** 获取对应类中对应方法名的个数*/
    int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
    if (count == 0) {
        throw new BeanDefinitionValidationException(
                "Invalid method override: no method with name '" + mo.getMethodName() +
                "' on class [" + getBeanClassName() + "]");
    }
    else if (count == 1) {
        // Mark override as not overloaded, to avoid the overhead of arg type checking.
        /** 标记MethodOverride暂未被覆盖，避免参数类型检查的开销*/
        mo.setOverloaded(false);
    }
}
```
&ensp;&ensp;在Spring中 将这两个方法 lockup-method 和
replace-method配置，统称为 method overrides。这两个方法的加载就是统一存放在
`BeanDefinition`中的 `methodOverrides` 属性中，这两个功能的实现原理就是在
`bean` 实例化的时候如果检测到该属性，会动态的为当前的 `bean` 生成代理，并使用拦截器为
`bean` 的增强做处理。

&ensp;&ensp;对于方法的匹配，如果一个类中存在若干个重载方法，那么在函数调用即增强的时候还需要根据
参数类型进行匹配，来最终确定当前调用的到底是那个函数。但是，Spring将一部分工作在这里完成了，如果当前
类中只有一个方法，那么就设置重载方法没有被重载，这样在后续调用的时候边可以直接使用找到的方法，而不需要
进行方法的参数匹配验证了，而且还可以提前对方法存在性进行验证。

#### bean实例化的前置处理器

&ensp;&ensp;代码片段：
```java
Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
if (bean != null) {
    // 在配置了PostProcessor的处理器中 改变了Bean，直接返回
    return bean;
}
```
**`resolveBeforeInstantiation()`方法**

```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
    Object bean = null;
    /** 如果尚未被解析*/
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
        // Make sure bean class is actually resolved at this point.
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            Class<?> targetType = determineTargetType(beanName, mbd);
            if (targetType != null) {
                // bean的前置处理器
                bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                if (bean != null) {
                    // bean的后置处理器
                    bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                }
            }
        }
        mbd.beforeInstantiationResolved = (bean != null);
    }
    return bean;
}
```
&ensp;&ensp;在这个方法中，涉及两个字方法的调用`applyBeanPostProcessorsBeforeInstantiation()`以及
`applyBeanPostProcessorsAfterInitialization()`，其中前者是对所有的`InstantiationAwareBeanPostProcessor`
类的子类中的`postProcessBeforeInstantiation()`方法的调用。后者，是对`BeanPostProcessor`中  
的`postProcessAfterInitialization()`方法的调用。

**实例化前的后置处理器应用**
```java
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
        throws BeansException {

    Object result = existingBean;
    for (BeanPostProcessor processor : getBeanPostProcessors()) {
        Object current = processor.postProcessBeforeInitialization(result, beanName);
        if (current == null) {
            return result;
        }
        result = current;
    }
    return result;
}
```

**实例化后的后置处理器应用**
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









