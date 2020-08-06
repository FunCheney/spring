### 万众期待的 doGetBean() 方法
&ensp;&ensp;Spring的方法学习了这么久，对于Spring的行事风格也有了一定的了解，看到 `doXXX` 的方法，要足够重视。这里的 `Bean`的实例化也不例外。上述的几个方法都是在
`doGetBean()` 方法中的子调用流程。下面我将试图从源码和`doGetBean()`时序图来进行分析：
#### 源码实现及分析
```java
protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
        @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

    // 转换beanName, FactoryBean 的name会已& 开头
    final String beanName = transformedBeanName(name);
    Object bean;

    /*
     * 先从缓存中取得Bean，处理那些已经被创建过的单例模式的Bean，这些Bean无需重复创建
     * 创建单例Bean的时候，会存在依赖注入的情况，创建依赖的时候为了避免循环依赖
     * spring 创建Bean的原则是不等Bean创建完就会将创建Bean的ObjectFactory提早曝光
     * 也就是将ObjectFactory加入待缓存中，一旦下一个Bean创建的时候需要依赖上个Bean则
     * 直接使用ObjectFactory
     */
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null && args == null) {
        if (logger.isTraceEnabled()) {
            if (isSingletonCurrentlyInCreation(beanName)) {
                logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                        "' that is not fully initialized yet - a consequence of a circular reference");
            }
            else {
                logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
            }
        }
        /*
         * getObjectForBeanInstance() 完成的是FactoryBean的相关处理，以取得FactoryBean的生产结果，
         * BeanFactory 和 FactoryBean的区别 查看之前的解释
         * 存在BeanFactory的情况并不是直接返回实例本身，而是返回指定方法返回的实例
         */
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }

    else {
        /*
         * 当前正在创建的类是否是prototype，如果是就报错
         * 只有在单例情况下才会尝试解决循环依赖
         */
        if (isPrototypeCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }

        /*
         * 判断IOC容器中的 BeanDefinition 是否存在，检查是否能在当前的BeanFactory中取得需要的Bean，
         * 如果当前的 BeanFactory 中取不到，则到双亲的 BeanFactory 中去取；
         * 如果当前的双亲工厂中取不到，就顺着双亲BeanFactory 链一直向上查找;
         * parentBeanFactory 如果不对 Spring 做处理，这里一般得到的 BeanFactory 都为空
         */
        BeanFactory parentBeanFactory = getParentBeanFactory();
        // parentBeanFactory 不为空 且  beanDefinitionMap 中不存在该name的 BeanDefinition
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // 确定原始 beanName
            String nameToLookup = originalBeanName(name);
            // 若为 AbstractBeanFactory 类型，委托父类处理
            if (parentBeanFactory instanceof AbstractBeanFactory) {
                return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                        nameToLookup, requiredType, args, typeCheckOnly);
            }
            // 递归到BeanFactory中寻找
            else if (args != null) {
                // Delegation to parent with explicit args.
                return (T) parentBeanFactory.getBean(nameToLookup, args);
            }
            else if (requiredType != null) {
                // No args -> delegate to standard getBean method.
                return parentBeanFactory.getBean(nameToLookup, requiredType);
            }
            else {
                return (T) parentBeanFactory.getBean(nameToLookup);
            }
        }

        // 类型检查
        if (!typeCheckOnly) {
            // 添加到 alreadyCreated 集合中
            markBeanAsCreated(beanName);
        }

        try {
            /**
             * 将存储在XML配置文件的 GernericBeanDefinition转换为RootBeanDefinition
             * 如果指定BeanName 是 子Bean的话，同时会合并父类相关的属性
             * 根据bean的名字 获取 BeanDefinition
             */
            final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

            checkMergedBeanDefinition(mbd, beanName, args);

            /**
             * 获取当前Bean的所有依赖Bean，在getBean()时递归调用。
             * 直到取到一个没有任何依赖的Bean为止
             */
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
                // 如从在依赖则需要递归实例化依赖的Bean
                for (String dep : dependsOn) {
                    // 检验依赖的bean 是否已经注册给当前 bean 获取其他传递依赖bean
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }
                    // 缓存依赖调用
                    registerDependentBean(dep, beanName);
                    try {
                        // 触发新的依赖
                        getBean(dep);
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                    }
                }
            }

            // Create bean instance.
            if (mbd.isSingleton()) {
                /**
                 * 通过createBean()方法创建singleton bean的实例，
                 * 这里通过拉姆达表达式创建 Object 对象
                 * 在getSingleton()中调用ObjectFactory 的createBean
                 */
                sharedInstance = getSingleton(beanName, () -> {
                    try {
                        return createBean(beanName, mbd, args);
                    }
                    catch (BeansException ex) {
                        // Explicitly remove instance from singleton cache: It might have been put there
                        // eagerly by the creation process, to allow for circular reference resolution.
                        // Also remove any beans that received a temporary reference to the bean.
                        destroySingleton(beanName);
                        throw ex;
                    }
                });
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            }

            /** 这里是创建prototype bean的地方*/
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

            else {
                String scopeName = mbd.getScope();
                final Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }
                try {
                    Object scopedInstance = scope.get(beanName, () -> {
                        beforePrototypeCreation(beanName);
                        try {
                            return createBean(beanName, mbd, args);
                        }
                        finally {
                            afterPrototypeCreation(beanName);
                        }
                    });
                    bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                }
                catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName,
                            "Scope '" + scopeName + "' is not active for the current thread; consider " +
                            "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                            ex);
                }
            }
        }
        catch (BeansException ex) {
            cleanupAfterBeanCreationFailure(beanName);
            throw ex;
        }
    }

    // Check if required type matches the type of the actual bean instance.
    /**
     * 这里对创建的Bean进行类型检查，
     * 如果没有问题，就返回这个新创建的Bean
     * 这个Bean 是已经包含了依赖关系的Bean
     */
    if (requiredType != null && !requiredType.isInstance(bean)) {
        try {
            T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
            if (convertedBean == null) {
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
            return convertedBean;
        }
        catch (TypeMismatchException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("Failed to convert bean '" + name + "' to required type '" +
                        ClassUtils.getQualifiedName(requiredType) + "'", ex);
            }
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
    }
    return (T) bean;
}
```
&ensp;&ensp;这个方法的内容，本片文章的要介绍的，已经在本文的前半部分做了介绍。这里只是，将整个方法拿出来，从整个框架上面看一下这个方法做了些什么事，大致总结为如下的
几个方面：

Bean加载过程中涉及的过程步骤

   ①：转换对应的BeanName
   
       这里传入的参数可能是 别名，也可能是FactoryBean，需要进行解析：
         a：去除FactoryBean的修饰符，也就是如果name="&aa"，首选会去掉"&"是name="aa"。
         b：取指定alias所表示的最终beanName，例如别名A指向名称为B的bean则返回B；
              如果别名A指向别名B，别名B有指向别名C的bean则返回C。
              
   ②：尝试存缓存中加载单例Bean
   
       单例在spring的同一个容器内只会被创建一次，后续在获取Bean，就直接从单例缓存中获取了。这里先尝试在缓存中加载，
       如果加载不成功则再次尝试从singletonFactories中加载，因为在创建单例bean的时候会存在依赖注入的情况，而在创
       建依赖的时候为了避免循环依赖，在Spring中创建Bean的原则，不是等Bean创建完成就会将创建Bean的ObjectFactory
       提早曝光到缓存中，一旦下一个Bean 创建的时候需要依赖上一个Bean则直接使用ObjectFactory。
   
   ③：bean的实例化
   
       如果在缓存中得到了Bean的原始状态，则需要对Bean实例化。缓存中存在的是Bean的原始状态，并不一定是最终想要的Bean
       例如：我们需要对工厂Bean进行处理，那么这里得到的其实是工厂Bean的初始状态，但我们真正需要的是工厂Bean中定义的
       factory-method方法中返回的Bean，而getObjectForBeanInstance()就是完成这个工作的。
   
   ④：原型模式的依赖检查
   
       只有在单例的情况下才会尝试解决循环依赖，如果存在A中有B的属性，B中有A的属性，那么当依赖注入的时候，就会产生当A还
       未创建完的时候因为对于B的创建再次返回创建A，造成循环依赖；isPrototypeCurrentlyInCreation(beanName)判断true
   
   ⑤：检测parentBeanFactory
   
       缓存中没有，直接转到父类工厂上去加载。代码判断 parentBeanFactory != null && !containsBeanDefinition(beanName)
       parentBeanFactory != null 这个判断显而易见；
       !containsBeanDefinition(beanName) 检测如果当前加载的XMl文件不包含beanName所对应的配置，就只能到parentBeanFactory
       中去尝试加载了，然后再去递归调用getBean()方法。
       
   ⑥：将存储XML配置文件的GenericBeanDefinition 转换为 RootBeanDefinition。
   
       因为在xml中读取到的Bean信息都存储在 GenericBeanDefinition 中，但是所有Bean的后续处理都是针对RootBeanDefinition的
       所以这里需要进行一个转换，转换的同时如果父类bean不为空的话，则会一并合并父类的属性
       
   ⑦：寻找依赖
   
       因为bean的初始化过程可能会用到某些属性，而某些属性可能是动态配置的，并且配置依赖与其他的Bean，这个时候就有必要先加载依赖的Bean
       所以在Spring的加载顺序中，在加载某一个Bean时会首先初始化这个bean所对应的依赖。
       
   ⑧：针对不同的scope进行Bean的创建
   
       根据bean作用域类型进行初始化
       
   ⑨：类型转换
   
       程序运行到这里bean的初始化基本结束，requiredType通常是为空的
       但是可能存在这样的情况，返回的Bean是个String，但是requiredType是Integer，这个时候这个步骤就起作用了：
       将返回的Bean转换为requiredType所指定的类型。

#### 时序图