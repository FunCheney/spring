## 通过 BeanPostProcessors 确定构造函数
### 1.开篇
&ensp;&ensp;上一篇文章学习了在Spring中如何通过工厂方法来实例化对象的，详细分析了 `createBeanInstance()` 方法中调用的 `instantiateUsingFactoryMethod(beanName, mbd, args)`
方法，今天这个片文章，将来分析另一个被调用的方法 `determineConstructorsFromBeanPostProcessors(beanClass, beanName)`。方法入口如下：

&ensp;&ensp;在这个的实现中，我们又看到了非常熟悉的 `BeanPostProcessors` 的处理，代码如下：  

```java
protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
        throws BeansException {

    if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
                if (ctors != null) {
                    return ctors;
                }
            }
        }
    }
    return null;
}
```
&ensp;&ensp;从上述代码中，可以看到 Spring 中的处理方式就是先获取到所有的 `BeanPostProcessors` 的子类实现，然后循环判断，如果有符合条件的，
那么就调用该类的 `determineCandidateConstructors()` 方法，完成相应的处理。


&ensp;&ensp;在文章正式开始之前，又到了问问题的时候了， `ibp.determineCandidateConstructors(beanClass, beanName
)` 在执行 `determineCandidateConstructors()` 方法的 `BeanPostProcessor` 的实现类中，有一个 `AutowiredAnnotationBeanPostProcessor`,
我这里有一个问题，就是这个类是在什么时候放到 `BeanDefinitionMap`中的呢？**请查看Spring容器初始化之先发五虎**。


### 2 你真的知道Spring如何选择构造器吗？
&ensp;&ensp;文章开始之前，还是要从使用场景入手，然后在通过源码来分析，毕竟源码是不会骗人的。。。

##### 情形一
```java
@Service
public class DemoServiceOne {

}
```
##### 情形二
```java
@Service
public class DemoServiceOne {
    @Autowired
	DemoServiceTwo demoServiceTwo;

    public DemoServiceOne(){}

    public DemoServiceOne(DemoServiceTwo demoServiceTwo){
    		this.demoServiceTwo = demoServiceTwo;
    }
}
```
##### 情形三

```java
@Service
public class DemoServiceOne {

	@Autowired
	DemoServiceTwo demoServiceTwo;

	@Autowired
	DemoServiceThree demoServiceThree;

	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	}

	public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
		this.demoServiceThree = demoServiceThree;
	}
}
```

##### 情形四
```java
@Service
public class DemoServiceOne {

	@Autowired
	DemoServiceTwo demoServiceTwo;

	@Autowired
	DemoServiceThree demoServiceThree;

	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	}
	@Autowired
	public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
		this.demoServiceThree = demoServiceThree;
	}
}
```

### 3 AutowiredAnnotationBeanPostProcessor 类中的方法 
&ensp;&ensp;从代码的实现可以看出，对于一个放在注册到容器中的 `BeanName`，都会做一次这个判断。终于没有交给Spring的类，这里当然是不会做处理了。

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

&ensp;&ensp;首先来分析一下上述代码主要住了什么事，采用伪代码的方式，挑主要的步骤进行说明：

①：首先获取类的构造方法，记录在 `Constructor<?>[] rawCandidates` 中国
    > rawCandidates = beanClass.getDeclaredConstructors();

②： `for` 循环 `rawCandidates` 对其中的每一个构造器进行判断

③：判断构造器上是否加了注解
   
   * 3.1 有加注解
      > 先判断requiredConstructor集合是否为空, 若不为空则代表之前已经有一个required=true的构造器了，两个true将抛出异常. 
        再判断candidates 集合是否为空，若不为空则表示之前已经有一个打了注解的构造器，若有required又是true，抛出异常.
        若上述判断都通过了，将当前构造器赋值给 requiredConstructor集合中，再放入candidates集合中。
   * 3.2 没有加注解             
      > 如果是无惨构造器则赋值给 defaultConstructor，其他的构造器不做处理
                                 
⑤：没有加注解且参数的个数为 0。将当前 `for` 循环的构造方法赋值给 `defaultConstructor`
    > defaultConstructor = candidate;
    
⑥：确定构造器
   

#### 4 情形分析

* 构造器上没有注解的情况： 

  + 无参构造器将直接加入defaultConstructor集合中，无论是否申明。但是本方法最后返回的是 `null`。最终的实例化是通过默认的构造函数来完成的 `instantiateBean(beanName, mbd)`。
  
  + 在构造器数量只有一个且有参数时，此唯一有参构造器将加入candidateConstructors集合中。最后返回。
  
  + 在构造器数量大于1个，无论是否申明无参构造器的情况下，将返回一个空的candidateConstructors集合，也就是没有找到构造器。
   > 不过这里需要区分一下是否声明无惨构造器：
       如果未声明  defaultConstructor 为null
       如果声明了 defaultConstructor 不为null

      
&ensp;&ensp;综上所述，在构造器没有注解的情况下，如果有且仅有一个非无惨的构造器，那么本方法返回的就是这个构造器。如果大于一个或则只有一个
无参构造器，那么该方法返回的都是 `null`。

                                                                                                                                                                                                                                                                                                                                                                    
* 构造器上有注解的情况：

&ensp;&ensp;构造器上有注解的情况需要判断required属性：

  + 两个构造器上都有 `@Autowired` 且 required属性都为true
    > 抛出异常
  
  + 两个构造器上都有 `@Autowired` 一个 required属性都为true 另一个 required属性都为false
    > 抛出异常
  
  + 两个构造器上都有 `@Autowired` 且 required属性都为false
    > 通过
   

### 5 总结
* 1. 对象中存在多个未被注解的构造器`determineCandidateConstructors()`方法都会返回`null`。***这里有彩蛋***
* 2. 对象中有且仅有一个未被注入的构造器 
     
     + 若该构造器为无参的，那么`determineCandidateConstructors()`返回 `null`
     + 若该构造哦器为有参的，那么`determineCandidateConstructors()`返回 该构造器
* 3. 仅有一个注入的构造器，则使用改构造器
* 4. 有两个注入的构造器
     + required 都为 `true` 抛出异常
     + required 一个为 `true` 另一个为 `false` 抛出异常
     + required 都为 `false` 正常通过
     
&ensp;&ensp;这篇文章中对Spring中通过使用 `BeanPostProcessor` 的实现类来完成构造器的确认进行了分析。当我们实例化一个对象的时候，构造方法确定了，
那么就可以通过使用构造方法来实例化了。但是，通过上面的分析发现，可能会有多个构造方法存在的情况，那么在这种情况下，Spring是如何确定使用哪个构造方法的，
也就是大名鼎鼎的推断构造器，下一篇文章中，将进行分析。
