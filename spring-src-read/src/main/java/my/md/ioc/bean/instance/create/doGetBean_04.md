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

 
