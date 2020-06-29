## Spring Bean 的实例化_04
&ensp;&ensp;上篇文章介绍到了 `singletonFactory.getObject()` 是一个机器复杂的过程，在这个过程中会完成 `Bean`的实例化。
今天这篇文章的开始就是在这里。在开始文章之前，我首先要做的是通过断点调试的方式来看看，方法的调用过程。
getSingleton方法调用栈.jpg

&ensp;&ensp;从上图中可以看出，在调用到 `singletonFactory.getObject()` 的时候，调用到 `createBean()`方法。今天方法的开始就是这里。

### createBean()
```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
        throws BeanCreationException {

    if (logger.isTraceEnabled()) {
        logger.trace("Creating instance of bean '" + beanName + "'");
    }
    RootBeanDefinition mbdToUse = mbd;

    /** 判断需要创建的Bean是否可以实例化，这个类是否可以通过类装载器载入 */
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    try {
        /**
         * 处理lookup-method 和 replace-method配置，spring将这两个方法统称为 method overrides
         */
        mbdToUse.prepareMethodOverrides();
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                beanName, "Validation of method overrides failed", ex);
    }

    try {
        // 如果Bean配置了PostProcessor，这里返回的是一个Proxy
        // 这里配置的后置处理器，返回的对象 不会维护对象之间的关系
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
         * 创建Bean的实例
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
&ensp;&ensp;这里的 `createBean()` 是以回调函数的形式在方法中执行的，我们发现，该方法主要完成的具体步骤即功能：

 *  ①：根据设置的 class 属性或者根据 className 来解析 Class;
 *  ②：对 override 属性进行标记处理及验证;
  > 这里对于 lookup-method 和 replace-method 的处理，我们暂时放一放。本着抓主要矛盾方式我们先继续向下看，这部分的知识点之后再学习。 

 * ③：应用初始换前的后置处理器，解析指定 `bean` 是否存在初始化前的短路操作;
   > 在实例化之前， 给 BeanPostProcessors 一个处理的机会，返回一个被代理的对象。这里如果返回的对象不为null，则直接返回。由此可知，这里返回的
     对象是不会维护对象之间的关系的。
                                              
 * ④：创建bean
   > doCreateBean 方法，当看到doXXX 的方法，在Spring 中不用多说就是比较重要的方法了，这个是真正创建 `Bean` 的地方。
 
&ensp;&ensp;这里我们简单解释一下 `resolveBeforeInstantiation`。这个方法设计 `BeanPostProcessor`的处理，而`BeanPostProcessor` 又是非常重要的。
### resolveBeforeInstantiation
```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
    Object bean = null;
    /** 如果尚未被解析*/
    if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
        // Make sure bean class is actually resolved at this point.
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            // 确定目标类型
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
&ensp;&ensp;上述方法中设计到两个子过程 `applyBeanPostProcessorsBeforeInstantiation` 和 `applyBeanPostProcessorsAfterInitialization`。
#### applyBeanPostProcessorsBeforeInstantiation 
```java
protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
    for (BeanPostProcessor bp : getBeanPostProcessors()) {
        if (bp instanceof InstantiationAwareBeanPostProcessor) {
            InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
            Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
            if (result != null) {
                return result;
            }
        }
    }
    return null;
}
```
&ensp;&ensp;可以看出，这里是一个循环处理的过程，先获取到所有的 `BeanPostProcessor` 的实现，然后判断为 `InstantiationAwareBeanPostProcessor`
的子类的话，就挨个调用 `postProcessBeforeInstantiation`。可以通过下图简单的看一下，具体的代码实现，这里就先不赘述了。

 postProcessBeforeInstantiation_01.jpg
 
 #### applyBeanPostProcessorsAfterInitialization
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
&ensp;&ensp;这个方法的执行，是在执行完 `applyBeanPostProcessorsBeforeInstantiation` 后所得对象不为 `null` 的情况下执行的。同样这里的
方法也不做赘述。

&ensp;&ensp;这里关于 `BeanPostProcessor` 的判断是，实现了 `InstantiationAwareBeanPostProcessor` 的类才会执行。通过实现 `InstantiationAwareBeanPostProcessor`
来干预 `Bean` 的生命周期。 之前提到的关于 `BeanPostProcessor`的子类的处理，是在初始化的时候插手初始化过程。这里实现了 `InstantiationAwareBeanPostProcessor` 是
插手 `Bean` 的实例化过程，这里的实例化就是值对象的创建。关于 `BeanPostProcessor` 来说，在Spring中的设计就是用来扩展对 `bean` 的处理。在Spring内部做了更加细节的处理，
区分了**初始化**与**实例化**。
```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return null;
	}

	@Deprecated
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}
```
&ensp;&ensp;从接口的定义中可以看出，接口中定义的都是 `default` 方法，都有自己的默认实现。`postProcessBeforeInstantiation` 这个是在对象创建之前
执行的方法，该方法默认返回 `null`。如果子类重写的话，可以按照自己需要的逻辑返回对象。若经过这一步返回的对象不为`null`，说明对象已经被创建了。
`postProcessAfterInstantiation` 是在对象创建完成之后做的，对上一步骤生成的对象做相应的处理。同样`postProcessPropertyValues`也对已经生成的
对象的属性进行修改。

BeanPostProcessor插手Bean实例化.jpg


