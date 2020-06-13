## invokeBeanFactoryPostProcessors
断点调试图一：

```java
private static void invokeBeanFactoryPostProcessors(
        Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

    for (BeanFactoryPostProcessor postProcessor : postProcessors) {
        /**
         * 根据不同的 BeanFactoryPostProcessor 实现
         *  去调用不同的 postProcessBeanFactory 方法
         *  ConfigurationClassPostProcessor 同时也是 BeanFactoryPostProcessor 的子类
         */
        postProcessor.postProcessBeanFactory(beanFactory);
    }
}
```
&ensp;&ensp;从上图中可以看出，这里的 `postProcessor` 对应的子类为 `ConfigurationClassPostProcessor`，下面的 `postProcessBeanFactory()`
方法对应子类的实现如下：
```java
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    int factoryId = System.identityHashCode(beanFactory);
    if (this.factoriesPostProcessed.contains(factoryId)) {
        throw new IllegalStateException(
                "postProcessBeanFactory already called on this post-processor against " + beanFactory);
    }
    this.factoriesPostProcessed.add(factoryId);
    if (!this.registriesPostProcessed.contains(factoryId)) {
        // BeanDefinitionRegistryPostProcessor hook apparently not supported...
        // Simply call processConfigurationClasses lazily at this point then.
        processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
    }
    /**
     * 配置类 产生 cglib 代理
     */
    enhanceConfigurationClasses(beanFactory);
    beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
}
```
&ensp;&ensp;上述方法中，主要的功能就是：①增强配置类；②向容器中添加 `BeanPsotProcessor` 的子类 `ImportAwareBeanPostProcessor`。
其中对于配置类的增强，是本文要介绍的，这个就和上一篇文章中提及Spring中对于配置类的 `Full`和`Lite` 模式的标记。
### 增强配置类
```java
public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
    Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
    for (String beanName : beanFactory.getBeanDefinitionNames()) {
        BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
        /**
         * 判断这个类是不是全注解类,这个地方与前面 {@link Configuration} 注解的类的
         * 处理有关
         */
        if (ConfigurationClassUtils.isFullConfigurationClass(beanDef)) {
            if (!(beanDef instanceof AbstractBeanDefinition)) {
                throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +
                        beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
            }
            else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
                logger.info("Cannot enhance @Configuration bean definition '" + beanName +
                        "' since its singleton instance has been created too early. The typical cause " +
                        "is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +
                        "return type: Consider declaring such methods as 'static'.");
            }
            /** 如果是全注解类，就将其 put 到 configBeanDefs 中*/
            configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
        }
    }
    if (configBeanDefs.isEmpty()) {
        /**
         * Map 为空 表示没有全注解类，则返回
         */
        // nothing to enhance -> return immediately
        return;
    }

    ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
    for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
        AbstractBeanDefinition beanDef = entry.getValue();
        // If a @Configuration class gets proxied, always proxy the target class
        beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
        try {
            // Set enhanced subclass of the user-specified bean class
            /**
             * 对全注解类 进行 cglib 代理
             * config 类 -> cglib class -> BeanDefinition -> bean
             */
            Class<?> configClass = beanDef.resolveBeanClass(this.beanClassLoader);
            if (configClass != null) {
                Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
                if (configClass != enhancedClass) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +
                                "enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
                    }
                    beanDef.setBeanClass(enhancedClass);
                }
            }
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
        }
    }
}
```
&ensp;&ensp;接上篇文章我们提到配置类的 `Full`和 `Lite`两种不同的模式。在这篇文章中，我们可以看到，`Full`模式，也就是全配置类，Spring 通过使用`CGLIB`动态代理的
方式对其进行了增强。而 `Lite` 模式的配置类，没有通过代理的方式增强。我们究其原因发现，对于`@Configuration` 类的处理，是Spring 的后置处理器的典型应用。纵观整个 Spring，
在器内部只有一个 `ConfigurationClassPostProcessor` 该类中处理了 `BeanDefinitionRegistryPostProcessor
` 的方法 `postProcessBeanDefinitionRegistry()`
也处理了 `postProcessBeanFactory`的方法 `postProcessBeanFactory()`。通过该类，我们应该也要知道，对于Spring 的扩展点 `BeanFactoryPostProcessor` 的处理。

&ensp;&ensp;对于配置类，先通过 `resolveBeanClass()` 解析得到类的`Class`对象，然后通多 `enhancer.enhance(configClass, this.beanClassLoader
)` 得到增强后的类对象。最后再将这个增强后的类对象，设置回对应的 `BeanDefinition`中去。

### CGLIB 代理增强
```java
public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
    /** 判断是否被代理过*/
    if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Ignoring request to enhance %s as it has " +
                    "already been enhanced. This usually indicates that more than one " +
                    "ConfigurationClassPostProcessor has been registered (e.g. via " +
                    "<context:annotation-config>). This is harmless, but you may " +
                    "want check your configuration and remove one CCPP if possible",
                    configClass.getName()));
        }
        return configClass;
    }
    /** 没有被代理 cglib 代理*/
    Class<?> enhancedClass = createClass(newEnhancer(configClass, classLoader));
    if (logger.isTraceEnabled()) {
        logger.trace(String.format("Successfully enhanced %s; enhanced class name is: %s",
                configClass.getName(), enhancedClass.getName()));
    }
    return enhancedClass;
}
```
#### newEnhancer() 方法
```java
/**
 * Creates a new CGLIB {@link Enhancer} instance.
 * 创建一个 CGLIB 实例
 */
private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
    Enhancer enhancer = new Enhancer();
    /** 增强父类 */
    enhancer.setSuperclass(configSuperClass);
    /** 增强接口，
     * 便于判断，表示一个类被增强了
     * EnhancedConfiguration 实现了 BeanFactoryAware 接口
     */
    enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
    enhancer.setUseFactory(false);
    /**
     * BeanFactoryAwareGeneratorStrategy 是一个生成策略
     * 主要为生成的 cglib 类中添加成员变量 $$beanFactory
     * 同时基于接口 EnhancedConfiguration 的父接口 BeanFactoryAware 中的 setBeanFactory 方法，
     * 设置此变量的值为当前 context 中的 beanFactory，这样一来 cglib 代理的对象就有了 beanFactory
     * 有了 factory 就能获得对象了，不用通过 new 来获取对象了
     * 该BeanFactory 的作用是在 this 调用时拦截该调用，并直接在 beanFactory 中获得目标bean
     *
     */
    enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
    enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
    enhancer.setCallbackFilter(CALLBACK_FILTER);
    enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
    return enhancer;
}
```
#### createClass()
```java
/**
 * 使用增强器生成超类的子类，
 * 确保新的子类注册了回调
 * Uses enhancer to generate a subclass of superclass,
 * ensuring that callbacks are registered for the new subclass.
 */
private Class<?> createClass(Enhancer enhancer) {
    Class<?> subclass = enhancer.createClass();
    // Registering callbacks statically (as opposed to thread-local)
    // is critical for usage in an OSGi environment (SPR-5932)...
    Enhancer.registerStaticCallbacks(subclass, CALLBACKS);
    return subclass;
}
```
&ensp;&ensp;其中 `CALLBACKS` 的定义如下：
```java
private static final Callback[] CALLBACKS = new Callback[] {
        /**
         * 增强方法，主要控制bean的作用域，
         * 不用每次都 new
         */
        // Bean 方法来拦截器
        new BeanMethodInterceptor(),
        // 拦截BeanFactoryAware 定义的方法 setBeanFactory
        new BeanFactoryAwareMethodInterceptor(),
        NoOp.INSTANCE
};
```
&ensp;&ensp;对应的 `new BeanMethodInterceptor()` 拦截方法调用，`new BeanFactoryAwareMethodInterceptor()` 拦截 `BeanFactoryAware`
 定义的方法 `setBeanFactory`。对应代码实现如下
 #### 拦截方法调用
```java
private static class BeanMethodInterceptor implements MethodInterceptor, ConditionalCallback {

		/**
		 * Enhance a {@link Bean @Bean} method to check the supplied BeanFactory for the
		 * existence of this bean object.
		 * @throws Throwable as a catch-all for any exception that may be thrown when invoking the
		 * super implementation of the proxied method i.e., the actual {@code @Bean} method
		 */
		@Override
		@Nullable
		public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
					MethodProxy cglibMethodProxy) throws Throwable {
			/**
			 * enhancedConfigInstance 代理
			 * 通过enhancedConfigInstance 中 CGLIB 生成的成员变量 &&beanFactory 获取 beanFactory
			 */
			ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
			String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);

			// Determine whether this bean is a scoped-proxy
			if (BeanAnnotationHelper.isScopedProxy(beanMethod)) {
				String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
				if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {
					beanName = scopedBeanName;
				}
			}

			// To handle the case of an inter-bean method reference, we must explicitly check the
			// container for already cached instances.

			// First, check to see if the requested bean is a FactoryBean. If so, create a subclass
			// proxy that intercepts calls to getObject() and returns any cached bean instance.
			// This ensures that the semantics of calling a FactoryBean from within @Bean methods
			// is the same as that of referring to a FactoryBean within XML. See SPR-6602.
			if (factoryContainsBean(beanFactory, BeanFactory.FACTORY_BEAN_PREFIX + beanName) &&
					factoryContainsBean(beanFactory, beanName)) {
				Object factoryBean = beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
				if (factoryBean instanceof ScopedProxyFactoryBean) {
					// Scoped proxy factory beans are a special case and should not be further proxied
				}
				else {
					// It is a candidate FactoryBean - go ahead with enhancement
					return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);
				}
			}
			/**
			 * 判断执行的方法 和 调用的方法是不是同一个方法
			 */
			if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
				// The factory is calling the bean method in order to instantiate and register the bean
				// (i.e. via a getBean() call) -> invoke the super implementation of the method to actually
				// create the bean instance.
				if (logger.isInfoEnabled() &&
						BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
					logger.info(String.format("@Bean method %s.%s is non-static and returns an object " +
									"assignable to Spring's BeanFactoryPostProcessor interface. This will " +
									"result in a failure to process annotations such as @Autowired, " +
									"@Resource and @PostConstruct within the method's declaring " +
									"@Configuration class. Add the 'static' modifier to this method to avoid " +
									"these container lifecycle issues; see @Bean javadoc for complete details.",
							beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));
				}
				/**
				 * 执行的方法和调用的方法是同一个 执行父类的方法 创建对象
				 */
				return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
			}
			/**
			 * 执行的方法和调用的方法不是同一个，在 &&BeanFactory 中 get 一个 bean 出来
			 */
			return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);
		}

		private Object resolveBeanReference(Method beanMethod, Object[] beanMethodArgs,
				ConfigurableBeanFactory beanFactory, String beanName) {

			// The user (i.e. not the factory) is requesting this bean through a call to
			// the bean method, direct or indirect. The bean may have already been marked
			// as 'in creation' in certain autowiring scenarios; if so, temporarily set
			// the in-creation status to false in order to avoid an exception.
			/**
			 * 判断对象是否正在创建
			 * 一个对象有三种状态
			 *   a. 没有创建
			 *   b. 正在创建
			 *   c. 创建成功
			 */
			boolean alreadyInCreation = beanFactory.isCurrentlyInCreation(beanName);
			try {
				if (alreadyInCreation) {
					beanFactory.setCurrentlyInCreation(beanName, false);
				}
				boolean useArgs = !ObjectUtils.isEmpty(beanMethodArgs);
				if (useArgs && beanFactory.isSingleton(beanName)) {
					// Stubbed null arguments just for reference purposes,
					// expecting them to be autowired for regular singleton references?
					// A safe assumption since @Bean singleton arguments cannot be optional...
					for (Object arg : beanMethodArgs) {
						if (arg == null) {
							useArgs = false;
							break;
						}
					}
				}
				/**
				 * 调用 beanFactory.getBean() 获取对象
				 */
				Object beanInstance = (useArgs ? beanFactory.getBean(beanName, beanMethodArgs) :
						beanFactory.getBean(beanName));
				if (!ClassUtils.isAssignableValue(beanMethod.getReturnType(), beanInstance)) {
					// Detect package-protected NullBean instance through equals(null) check
					if (beanInstance.equals(null)) {
						if (logger.isDebugEnabled()) {
							logger.debug(String.format("@Bean method %s.%s called as bean reference " +
									"for type [%s] returned null bean; resolving to null value.",
									beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
									beanMethod.getReturnType().getName()));
						}
						beanInstance = null;
					}
					else {
						String msg = String.format("@Bean method %s.%s called as bean reference " +
								"for type [%s] but overridden by non-compatible bean instance of type [%s].",
								beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
								beanMethod.getReturnType().getName(), beanInstance.getClass().getName());
						try {
							BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
							msg += " Overriding bean of same name declared in: " + beanDefinition.getResourceDescription();
						}
						catch (NoSuchBeanDefinitionException ex) {
							// Ignore - simply no detailed message then.
						}
						throw new IllegalStateException(msg);
					}
				}
				Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
				if (currentlyInvoked != null) {
					String outerBeanName = BeanAnnotationHelper.determineBeanNameFor(currentlyInvoked);
					beanFactory.registerDependentBean(beanName, outerBeanName);
				}
				return beanInstance;
			}
			finally {
				if (alreadyInCreation) {
					beanFactory.setCurrentlyInCreation(beanName, true);
				}
			}
		}
}
```
 通过增强以后，配置类中使用@Bean注解的bean定义方法就不再是普通的方法了，它们具有了如下跟bean作用域有关的能力，以单例bean为例 ：
 1. 它们首次被调用时，相应方法逻辑会被执行用于创建bean实例；
 2. 再次被调用时，不会再执行创建bean实例，而是根据bean名称返回首次该方法被执行时创建的bean实例。




