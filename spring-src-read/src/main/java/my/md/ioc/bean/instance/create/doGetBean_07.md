### 确定采用的构造方法
```java
public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
        throws BeanCreationException {

    // Let's check for lookup methods here...
    if (!this.lookupMethodsChecked.contains(beanName)) {
        try {
            ReflectionUtils.doWithMethods(beanClass, method -> {
                Lookup lookup = method.getAnnotation(Lookup.class);
                if (lookup != null) {
                    Assert.state(this.beanFactory != null, "No BeanFactory available");
                    LookupOverride override = new LookupOverride(method, lookup.value());
                    try {
                        RootBeanDefinition mbd = (RootBeanDefinition)
                                this.beanFactory.getMergedBeanDefinition(beanName);
                        mbd.getMethodOverrides().addOverride(override);
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanCreationException(beanName,
                                "Cannot apply @Lookup to beans without corresponding bean definition");
                    }
                }
            });
        }
        catch (IllegalStateException ex) {
            throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
        }
        this.lookupMethodsChecked.add(beanName);
    }

    // Quick check on the concurrent map first, with minimal locking.
    // 从缓存中查找
    Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
    // 缓存中没有
    if (candidateConstructors == null) {
        // 同步代码块
        synchronized (this.candidateConstructorsCache) {
            candidateConstructors = this.candidateConstructorsCache.get(beanClass);
            if (candidateConstructors == null) {
                Constructor<?>[] rawCandidates;
                try {
                    // 获取 Bean的所有构造器
                    rawCandidates = beanClass.getDeclaredConstructors();
                }
                catch (Throwable ex) {
                    throw new BeanCreationException(beanName,
                            "Resolution of declared constructors on bean Class [" + beanClass.getName() +
                            "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
                }
                List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
                //存放依赖注入的required=true的构造器
                Constructor<?> requiredConstructor = null;
                //存放默认构造器
                Constructor<?> defaultConstructor = null;
                //获取主要的构造器
                Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(beanClass);
                int nonSyntheticConstructors = 0;
                for (Constructor<?> candidate : rawCandidates) {
                    if (!candidate.isSynthetic()) {
                        nonSyntheticConstructors++;
                    }
                    else if (primaryConstructor != null) {
                        continue;
                    }
                    //查找当前构造器上的注解
                    AnnotationAttributes ann = findAutowiredAnnotation(candidate);
                    if (ann == null) {
                        // 没有注解
                        Class<?> userClass = ClassUtils.getUserClass(beanClass);
                        if (userClass != beanClass) {
                            try {
                                Constructor<?> superCtor =
                                        userClass.getDeclaredConstructor(candidate.getParameterTypes());
                                ann = findAutowiredAnnotation(superCtor);
                            }
                            catch (NoSuchMethodException ex) {
                                // Simply proceed, no equivalent superclass constructor found...
                            }
                        }
                    }
                    if (ann != null) {
                        // 有注解
                        if (requiredConstructor != null) {
                            //已经存在一个required=true的构造器了，抛出异常
                            throw new BeanCreationException(beanName,
                                    "Invalid autowire-marked constructor: " + candidate +
                                    ". Found constructor with 'required' Autowired annotation already: " +
                                    requiredConstructor);
                        }
                        //判断此注解上的required属性
                        boolean required = determineRequiredStatus(ann);
                        if (required) {
                            if (!candidates.isEmpty()) {
                                throw new BeanCreationException(beanName,
                                        "Invalid autowire-marked constructors: " + candidates +
                                        ". Found constructor with 'required' Autowired annotation: " +
                                        candidate);
                            }
                            //若为true
                            //将当前构造器赋值给 requiredConstructor
                            requiredConstructor = candidate;
                        }
                        // 当前的构造器添加到候选的构造器集合中
                        candidates.add(candidate);
                    }
                    //如果该构造函数上没有注解，再判断构造函数上的参数个数是否为0
                    else if (candidate.getParameterCount() == 0) {
                        //如果没有参数，加入defaultConstructor集合
                        defaultConstructor = candidate;
                    }
                }
                //适用的构造器集合若不为空
                if (!candidates.isEmpty()) {
                    // Add default constructor to list of optional constructors, as fallback.
                    //若没有required=true的构造器
                    if (requiredConstructor == null) {
                        if (defaultConstructor != null) {
                            //将defaultConstructor集合的构造器加入适用构造器集合
                            candidates.add(defaultConstructor);
                        }
                        else if (candidates.size() == 1 && logger.isInfoEnabled()) {
                            logger.info("Inconsistent constructor declaration on bean with name '" + beanName +
                                    "': single autowire-marked constructor flagged as optional - " +
                                    "this constructor is effectively required since there is no " +
                                    "default constructor to fall back to: " + candidates.get(0));
                        }
                    }
                    //将适用构造器集合赋值给将要返回的构造器集合
                    candidateConstructors = candidates.toArray(new Constructor<?>[0]);
                }
                //如果适用的构造器集合为空，且Bean只有一个构造器并且此构造器参数数量大于0
                else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
                    //就使用此构造器来初始化
                    candidateConstructors = new Constructor<?>[] {rawCandidates[0]};
                }
                //如果构造器有两个，且默认构造器不为空
                else if (nonSyntheticConstructors == 2 && primaryConstructor != null &&
                        defaultConstructor != null && !primaryConstructor.equals(defaultConstructor)) {
                    //使用默认构造器返回
                    candidateConstructors = new Constructor<?>[] {primaryConstructor, defaultConstructor};
                }
                else if (nonSyntheticConstructors == 1 && primaryConstructor != null) {
                    candidateConstructors = new Constructor<?>[] {primaryConstructor};
                }
                else {
                    // 都不符合 初始化 candidateConstructors 若不初始化 后面的判断会有问题
                    candidateConstructors = new Constructor<?>[0];
                }
                //放入缓存，方便下一次调用，不需要上述的解析过程了，直接在缓存中获取
                this.candidateConstructorsCache.put(beanClass, candidateConstructors);
            }
        }
    }
    // 这里可以看到对 candidateConstructors 的初始化时有意义的
    return (candidateConstructors.length > 0 ? candidateConstructors : null);
}
```
从这段核心代码我们可以看出几个要点：

在没有@Autowired注解的情况下：
无参构造器将直接加入defaultConstructor集合中。
在构造器数量只有一个且有参数时，此唯一有参构造器将加入candidateConstructors集合中。
在构造器数量有两个的时候，并且存在无参构造器，将defaultConstructor（第一条的无参构造器）放入candidateConstructors集合中。
在构造器数量大于两个，并且存在无参构造器的情况下，将返回一个空的candidateConstructors集合，也就是没有找到构造器。
在有@Autowired注解的情况下：
判断required属性：
true：先判断requiredConstructor集合是否为空，若不为空则代表之前已经有一个required=true的构造器了，两个true将抛出异常，再判断candidates集合是否为空，若不为空则表示之前已经有一个打了注解的构造器，此时required又是true，抛出异常。若两者都不为空将放入requiredConstructor集合中，再放入candidates集合中。
false：直接放入candidates集合中。
判断requiredConstructor集合是否为空（是否存在required=true的构造器），若没有，将默认构造器也放入candidates集合中。
最后将上述candidates赋值给最终返回的candidateConstructors集合。
4.总结

综上所述，我们可以回答开篇疑问点小结所总结的一系列问题了：

为什么写三个构造器（含有无参构造器），并且没有@Autowired注解，Spring总是使用无参构造器实例化Bean？

答：参照没有注解的处理方式： 若构造器只有两个，且存在无参构造器，将直接使用无参构造器初始化。若大于两个构造器，将返回一个空集合，也就是没有找到合适的构造器，那么参照第三节初始化Bean的第一段代码createBeanInstance方法的末尾，将会使用无参构造器进行实例化。这也就解答了为什么没有注解，Spring总是会使用无参的构造器进行实例化Bean，并且此时若没有无参构造器会抛出异常，实例化Bean失败。

为什么注释掉两个构造器，留下一个有参构造器，并且没有@Autowired注解，Spring将会使用构造器注入Bean的方式初始化Bean？

答：参照没有注解的处理方式： 构造器只有一个且有参数时，将会把此构造器作为适用的构造器返回出去，使用此构造器进行实例化，参数自然会从IOC中获取Bean进行注入。

为什么写三个构造器，并且在其中一个构造器上打上@Autowired注解，就可以正常注入构造器？

答：参照有注解的处理方式： 在最后判断candidates适用的构造器集合是否为空时，若有注解，此集合当然不为空，且required=true，也不会将默认构造器集合defaultConstructor加入candidates集合中，最终返回的是candidates集合的数据，也就是这唯一一个打了注解的构造器，所以最终使用此打了注解的构造器进行实例化。

两个@Autowired注解就会报错，一定需要在所有@Autowired中的required都加上false即可正常初始化？

答：参照有注解的处理方式： 当打了两个@Autowired注解，也就是两个required都为true，将会抛出异常，若是一个为true，一个为false，也将会抛出异常，无论顺序，因为有两层的判断，一个是requiredConstructor集合是否为空的判断，一个是candidates集合为空的判断，若两个构造器的required属性都为false，不会进行上述判断，直接放入candidates集合中，并且在下面的判断中会将defaultConstructor加入到candidates集合中，也就是candidates集合有三个构造器，作为结果返回。

至于第四条结论，返回的构造器若有三个，Spring将如何判断使用哪一个构造器呢？在后面Spring会遍历三个构造器，依次判断参数是否是Spring的Bean（是否被IOC容器管理），若参数不是Bean，将跳过判断下一个构造器，也就是说，例如上述两个参数的构造器其中一个参数不是Bean，将判断一个参数的构造器，若此参数是Bean，使用一个参数的构造器实例化，若此参数不是Bean，将使用无参构造器实例化。也就是说，若使用@Autowired注解进行构造器注入，required属性都设置为false的话，将避免无Bean注入的异常，使用无参构造器正常实例化。若两个参数都是Bean，则就直接使用两个参数的构造器进行实例化并获取对应Bean注入构造器。

在这里最后说一点，从上面可以看出，若想使用构造器注入功能，最好将要注入的构造器都打上@Autowired注解（若有多个需要注入的构造器，将所有@Autowired中required属性都设置为false），若有多个构造器，只有一个构造器需要注入，将这个构造器打上@Autowired注解即可，不用设置required属性。如果不打注解也是可以使用构造器注入功能的，但构造器数量只能为1，且代码可读性较差，读代码的人并不知道你这里使用了构造器注入的方式，所以这里我建议若使用构造器注入打上@Autowired注解会比较好一点。
