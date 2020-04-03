## Spring依赖注入之createBean()

### `doCreateBean()`
&ensp;&ensp;当经过`resolveBeforeInstantiation()`方法的处理之后，spring的处理方式有
两种，如果创建了代理或者重写了 `InstantiationAwareBeanPostProcessor`的
`postProcessBeforeInstantiation()`方法并在改方法中改变了
`bean`，则直接返回。否则就要进入到下面的 `doCreateBean()`中进行创建。
```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
			throws BeanCreationException {

    // BeanWrapper 是用来持有创建出来的Bean对象
    BeanWrapper instanceWrapper = null;
    if (mbd.isSingleton()) {
        // 如果是singleton，先清除缓存中同名的Bean
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    if (instanceWrapper == null) {
        /*
         * 通过createBeanInstance 来完成Bean所包含的java对象的创建。
         * 对象的生成有很多种不同的方式，可以通过工厂方法生成，
         * 也可以通过容器的autowire特性生成，这些生成方式都是由BeanDefinition来指定的
         */
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    /* getWrappedInstance() 获得原生对象*/
    final Object bean = instanceWrapper.getWrappedInstance();
    Class<?> beanType = instanceWrapper.getWrappedClass();
    if (beanType != NullBean.class) {
        mbd.resolvedTargetType = beanType;
    }

    // Allow post-processors to modify the merged bean definition.
    synchronized (mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            try {
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Post-processing of merged bean definition failed", ex);
            }
            mbd.postProcessed = true;
        }
    }

    // Eagerly cache singletons to be able to resolve circular references
    // even when triggered by lifecycle interfaces like BeanFactoryAware.
    /*
     * 是否需要提前曝光：单例 & 允许循环依赖& 当前的bean正在创建中，检测循环依赖
     */
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
            isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        if (logger.isTraceEnabled()) {
            logger.trace("Eagerly caching bean '" + beanName +
                    "' to allow for resolving potential circular references");
        }
        /*
         * setter 方法注入的Bean，通过提前暴露一个单例工厂方法
         * 从而能够使其他Bean引用到该Bean，注意通过setter方法注入的
         * Bean 必须是单例的才会到这里来。
         * 对 Bean 再一次依赖引用，主要应用 SmartInstantiationAwareBeanPostProcessor
         * 其中 AOP 就是在这里将advice动态织入bean中，若没有则直接返回，不做任何处理
         *
         * 在Spring中解决循环依赖的方法：
         *   在 B 中创建依赖 A 时通过 ObjectFactory 提供的实例化方法来中断 A 中的属性填充，
         *   使 B 中持有的 A 仅仅是刚初始化并没有填充任何属性的 A，初始化 A 的步骤是在最开始创建A的时候进行的，
         *   但是 因为 A 与 B 中的 A 所表示的属性地址是一样的，所以在A中创建好的属性填充自然可以通过B中的A获取，
         *   这样就解决了循环依赖。
         */
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // Initialize the bean instance.
    /*
     * 将原生对象复制一份 到 exposedObject，这个exposedObject在初始化完成处理之后
     * 会作为依赖注入完成后的Bean
     */
    Object exposedObject = bean;
    try {
        /* Bean的依赖关系处理过程*/
        populateBean(beanName, mbd, instanceWrapper);
        /* 将原生对象变成代理对象*/
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    catch (Throwable ex) {
        if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
            throw (BeanCreationException) ex;
        }
        else {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
        }
    }

    if (earlySingletonExposure) {
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                String[] dependentBeans = getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                for (String dependentBean : dependentBeans) {
                    // 检测循环依赖
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }
                /*
                 * 因为Bean创建后其所依赖的Bean一定是已经创建的，
                 * actualDependentBeans 不为空则表示当前bean创建后其依赖的Bean却没有全部创建完，
                 * 也就是说存在循环依赖
                 */
                if (!actualDependentBeans.isEmpty()) {
                    throw new BeanCurrentlyInCreationException(beanName,
                            "Bean with name '" + beanName + "' has been injected into other beans [" +
                            StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                            "] in its raw version as part of a circular reference, but has eventually been " +
                            "wrapped. This means that said other beans do not use the final version of the " +
                            "bean. This is often the result of over-eager type matching - consider using " +
                            "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                }
            }
        }
    }

    // Register bean as disposable.
    try {
        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }

    return exposedObject;
}
```
&ensp;&ensp;上述方法的处理过程大体总结如下：

①：如果是单例首先需要清除缓存

②：实例化 bean，将 BeanDefinition 转化为 BeanWrapper

&ensp;&ensp;Bean的实例化，是一个及其负载的过程，首先做一个总结，详细介绍[见文章1]。总结如下：

* 如果存在工厂方法，则使用工厂方法初始化；
* 如果一个类有多个构造函数，每个构造函数都有不同的参数，需要根据参数锁定构造函数并进行初始化。
* 如果即不存在工厂方法也不存在带有参数的构造方法，使用默认的构造函数进行初始化。

③：MergedBeanDefinitionPostProcessor 的应用

&ensp;&ensp; `bean`合并后的处理，Autowired注解正是通过此方法实现，如类型的预解析。

④：依赖处理

&ensp;&ensp;在Spring中会处理循环依赖的情况。如果从在循环依赖，且`bean`都是单例的，那么Spring
处理的方式就是当创建Bean时，通过放入缓存中ObjectFactory来创建实例，这样来解决循环依赖。

⑤：属性填充。将属性填充到bean的实例中。

⑥：循环依赖处理

&ensp;&ensp;对于property的bean，Spring不会去解决循环依赖，直接抛出异常。单例的Bean，spring会解决
循环依赖的问题。

⑦：注册 DisposableBean

&ensp;&ensp;如果配置了 `destroy-method`，这里需要注册，以便在销毁的时候调用。

⑧：完成创建并返回

[见文章1]:https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/md/ioc/bean/instance/create/Spring%E4%BE%9D%E8%B5%96%E6%B3%A8%E5%85%A5%E4%B9%8BcreateBean()_5.md


