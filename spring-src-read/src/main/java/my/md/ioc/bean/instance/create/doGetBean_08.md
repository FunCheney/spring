>其实生活中的坑，都是自己挖的，迷茫也是。愿我们内心坚定而且不失热爱，期待与你的共同进步。
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
&ensp;&ensp;在这篇文章中，我将详细的分析，在一个对象中通过 `@Autowired`注入或 `@Resource` 注入属性的处理过程。这里我还是采取使用情形，然后画出简要
流程图，最后再是源码分析的方式来介绍本文所要涉及的知识点。
#### 2.1 日常开发中注入对象的方式
**情形一**：通过 `@Autowired` 注解对象的方式

```java
@Service
public class DemoServiceTwo {

	@Autowired
	DemoServiceThree demoServiceThree;
}

```
**情形二**：通过 `@Autowired` 注解构造器的方式	

```java
@Service
public class DemoServiceTwo {
	
    DemoServiceOne demoServiceOne;
    
    @Autowired
    public DemoServiceTwo(DemoServiceOne demoServiceOne){
        this.demoServiceOne = demoServiceOne;
    } 
}
```

**情形三**：通过 `@Resource` 注解对象的方式

```java
@Service
public class DemoServiceTwo {

	@Resource
    DemoServiceOne demoServiceOne;
}
```

**情形四**：通过 `@Autowired` 注解方法的方式

```java
@Service
public class DemoServiceTwo {

    DemoServiceOne demoServiceOne;
    
    @Autowired 
    public void prepare(DemoServiceOne demoServiceOne){
        this.demoServiceOne = demoServiceOne;
    }
}
```

&ensp;&ensp;上述的四种方式是我们在日常开发中经常用到的注入对象的方式。这四种方式，在 Spring 对应不同的处理逻辑。

#### 2.2 对象之间依赖关系处理流程
![对象之间依赖关系处理流程](https://imgkr.cn-bj.ufileos.com/634d8ead-7d6c-4ee8-ad8e-25ed910b5965.jpg)

1. 上图中描述了前面 **2.1** 中所介绍的四种情形的处理，其中蓝色线条所表示的是 `@Resource`注解的处理过程。
2. 红色线条表示 `@Autowired`注解的处理过程，与之对应的有拆分成三种子情况
   
   * `AutowiredFieldElement` 表示注解属性的情况
   * `AutowiredMethodElement` 表示注解方法的情况
   * 绿颜色的线条表示注解在构造方法上的情况
   
&ensp;&ensp;通过上述流程图，我从中找到了以下几点。通过已下几点我们也可以区分 `@Resource` 和 `@Autowired`。

1. 两种注解的处理方式都是通过后置处理器来完成处理的，`getBeanPostProcessors()` 在我们不做任何扩展的情况下，Spring 中只有五个。如有忘记请查看：[**容器初始化先发五虎**](https://juejin.im/post/5ec9f01d6fb9a047a6445123)；
2. 对于 `@Resource` 的处理是通过 `CommonAnnotationBeanPostProcessor` 来完成的。
3. 对于 `@Autowired` 的处理是通过 `AutowiredAnnotationBeanPostProcessor`来处理的。
![两种注解对应的后置处理器](https://imgkr.cn-bj.ufileos.com/6e76e990-338d-40d7-a71d-6ff8cda68e5c.jpg)

4. 对于 `@Autowired` 注解构造器的方式，获取到被注解元素为 `null` 则直接返回。完成 `populateBean()` 的过程。
5. 对于剩下的情形，处理思路一致，都是先获取到被注入的对象，然后将维护对象属性之间的关系。
6. 重点突出一下 `getBean()` 这里还是我们熟悉的 `getBean()`...
7. `field.set()` 维护对象之间的依赖关系。

### 3.源码分析

&ensp;&ensp;源码分析首当其冲的就是方法入口，在对象的包装 `BeanWrapper` 创建完成之后，`populateBean()`来处理对象之间的依赖关系：
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
     * 或者通过 setAutowireMode() 的方式设置自动装配的模式
     */
    int resolvedAutowireMode = mbd.getResolvedAutowireMode();
    // Spring 默认 既不是 byType 也不是 byName, 默认是null
    // 这里之所以做这个判断是因为某种特殊的场景下，会修改到自动注入的模型，所以需要做判断
    if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
        /**
         * byName 处理
         * 通过反射从当前Bean中得到需要注入的属性名，
         * 然后使用这个属性名向容器申请与之同名的Bean，
         * 这样实际又触发了另一个Bean生成和依赖注入的过程
         */
        if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }
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
&ensp;&ensp;通过源码实现可以看出**自动装配**与**注解注入**的处理是有差别的。其中自动装配时通过属性判断来完成。注解注入是通过**后置处理器**来完成的。

&ensp;&ensp;不同的后置处理器的`postProcessProperties()`方法对应的是不同的处理逻辑。
### 4. @Autowired 注解属性
#### 4.1 处理过程
&ensp;&ensp;首先，通过 `findAutowiringMetadata()` 方法获取被注入的元数据。以上述：**情形一**，为例：
![](https://imgkr.cn-bj.ufileos.com/ef00cb1b-5832-4a5e-85c6-04c6ef49840c.jpg)

&ensp;&ensp;然后，在`inject()`方法中做相应的处理:

```java
public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
		Collection<InjectedElement> checkedElements = this.checkedElements;
		// 获取被注入的元素
		Collection<InjectedElement> elementsToIterate =
				(checkedElements != null ? checkedElements : this.injectedElements);
		if (!elementsToIterate.isEmpty()) {
			// 循环被注入的元素，调用 inject 方法
			for (InjectedElement element : elementsToIterate) {
				if (logger.isTraceEnabled()) {
					logger.trace("Processing injected element of bean '" + beanName + "': " + element);
				}
				// 调用注入方法
				element.inject(target, beanName, pvs);
			}
		}
	}
```
&ensp;&ensp;在这个方法中首先会检查注入的对象，这里需要指出，在 `@Autowired` 注解构造器的方式下，最终得到的 `elementsToIterate` 是空。

&ensp;&ensp;对于`@Autowired`注解的其他使用方式，最终都会调用 `element.inject(target, beanName, pvs);` 
![@Autowired注解的处理](https://imgkr.cn-bj.ufileos.com/f2ed9631-f97d-473f-b98d-1a4a3dd83fd1.jpg)

&ensp;&ensp;可以看出，这里区分了注解方法与注解属性这两种方式，在本文中，将以注解
属性的方式为例继续展开分析。

&ensp;&ensp;在 `AutowiredFieldElement`中类中的最终通过 `beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter)` 返回注入的对象，然后通过 `field.set(bean, value);`的方式来维护对象与属性之间的关系。

&ensp;&ensp;接下来将分析 `resolveDependency()`方法，通过该方法可以发现，最终调用的是 `doResolveDependency()`。看到了 `doXXXX()`的方法，就又到了 Spring 惯用的套路了，这里这方法就是真正做事的方式。下面就来看看在 `doResolveDependency()` 中做了什么事情...

```java
public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

    InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
    try {
        Object shortcut = descriptor.resolveShortcut(this);
        if (shortcut != null) {
            return shortcut;
        }

        /**
         * 根据类型获取，@Autowired 默认根据type
         */
        Class<?> type = descriptor.getDependencyType();
        /**
         * 支持 @value 注解
         */
        Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
        if (value != null) {
            if (value instanceof String) {
                String strVal = resolveEmbeddedValue((String) value);
                BeanDefinition bd = (beanName != null && containsBean(beanName) ?
                        getMergedBeanDefinition(beanName) : null);
                value = evaluateBeanDefinitionString(strVal, bd);
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            try {
                /** 通过转换器将Bean的值转换为对应的type类型*/
                return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
            }
            catch (UnsupportedOperationException ex) {
                // A custom TypeConverter which does not support TypeDescriptor resolution...
                return (descriptor.getField() != null ?
                        converter.convertIfNecessary(value, type, descriptor.getField()) :
                        converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
            }
        }

        Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
        if (multipleBeans != null) {
            return multipleBeans;
        }

        /**
         * 根据属性类型找到beanFactory中所有类型匹配的Bean
         * 返回值的结构为：
         * key：匹配的BeanName；
         * value：beanName 对应的实例化后的bean 通过 getBean(beanName)返回
         */
        Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
        if (matchingBeans.isEmpty()) {
            /**
             * 如果 autowire 的 require 属性为true
             * 找到的匹配项为空 则 抛出异常
             */
            if (isRequired(descriptor)) {
                raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
            }
            return null;
        }

        String autowiredBeanName;
        Object instanceCandidate;

        // 根据类型匹配到的数量大于 1个
        if (matchingBeans.size() > 1) {
            // 确定自动注入的beanName
            autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
            if (autowiredBeanName == null) {
                if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
                    return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
                }
                else {
                    return null;
                }
            }
            instanceCandidate = matchingBeans.get(autowiredBeanName);
        }
        else {
            // We have exactly one match.
            Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
            autowiredBeanName = entry.getKey();
            instanceCandidate = entry.getValue();
        }

        if (autowiredBeanNames != null) {
            autowiredBeanNames.add(autowiredBeanName);
        }
        if (instanceCandidate instanceof Class) {
            //实例化对象
            instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
        }
        Object result = instanceCandidate;
        if (result instanceof NullBean) {
            if (isRequired(descriptor)) {
                raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
            }
            result = null;
        }
        if (!ClassUtils.isAssignableValue(type, result)) {
            throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
        }
        return result;
    }
    finally {
        ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
    }
}
```
&ensp;&ensp;分析上述代码，可以发现其中主要做了如下的几件事：

①：获取注入对象的类型；

②：解析过程中对 `@value` 注解支持；

③：通过 `findAutowireCandidates()` 方法，根据属性类型找到 `BeanFactory` 中所有类型匹配的 `Bean`。存放在 `Map`中返回。在该方法中，会根据给定的类型获取所有 `Bean` 的名称作为 `Map`中的 `key`。

  * 对类型匹配的 `Bean` 做相应的判断，如果大于 1 个，则通过 `determineAutowireCandidate()` 方法来确定注入 `Bean`的名称。
     ![确定要注入的Bean](https://imgkr.cn-bj.ufileos.com/20e99f51-b094-422e-ad3c-1870ed16c8d3.jpg)
     + 首先根据 `@Primary` 注解的对象来确定，如果有则返回；
     + 然后在通过 `@Priority` 注解的对象，如果有则返回。
     
  * 如果等于 1 个，在返回 `Map` 中的 `key` 就是 `beanName`；
  
  &ensp;&ensp;通过上述的描述发现 `@Autowired` 注解属性的方式先通过 `byType` 的方式获取对应类型的对象；当对应类型的对象大于 1 个时，通过 `byName` 的方式来确定。
  
④：最后 `descriptor.resolveCandidate(autowiredBeanName, type, this);` 通过 `beanFactory.getBean(beanName);` 获取注入的对象。

#### 4.2 对象之间依赖关系的维护
&ensp;&ensp;在通过 `beanFactory.resolveDependency()` 方法获得依赖的对象之后，通过 `registerDependentBeans()` 方法来维护对象之间的依赖关系。
![对象之间依赖关系的维护](https://imgkr.cn-bj.ufileos.com/f712105b-24dc-41de-8351-9db9133b6223.jpg)

```java
private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
  if (beanName != null) {
    for (String autowiredBeanName : autowiredBeanNames) {
      if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
        this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
      }
      if (logger.isTraceEnabled()) {
        logger.trace("Autowiring by type from bean name '" + beanName +
            "' to bean named '" + autowiredBeanName + "'");
      }
    }
  }
}
```
&ensp;&ensp;上述代码中 `for` 循环所有 `@Autowired` 注入的属性的名称。判断容器中包含 `BeanName` 然后调用 `this.beanFactory.registerDependentBean(autowiredBeanName, beanName)`。`Spring` 通过下述的方法来维护了对象之间的依赖关系。

```java
public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

		/**
		 * dependentBeanMap中存储的是目前已经注册的依赖这个bean的所有bean,
		 * 这里从这个集合中获取目前所有已经注册的依赖beanName的bean集合,
		 * 然后看这个集合中是否包含dependentBeanName,即是否已经注册,
		 * 如果包含则表示已经注册,则直接返回;
		 * 否则,将bean依赖关系添加到两个map缓存即完成注册.
		 */
		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}
```
&ensp;&ensp;上述的代码中，有两个 `Map`。这里首先对这两个 `Map`稍加解释：
```java
/** 指定的bean与目前已经注册的依赖这个指定的bean的所有依赖关系的缓存（我依赖的）*/
private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

/** 指定bean与目前已经注册的创建这个bean所需依赖的所有bean的依赖关系的缓存（依赖我的) */
private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);
```
&ensp;&ensp;在上述的方法中，就是通过上述两个 `Map` 维护了对象间依赖与被依赖的关系，详细看下图
![对象之间依赖关系的维护](https://imgkr.cn-bj.ufileos.com/dba38750-472f-4019-a2d6-82907615c738.jpg)
&ensp;&ensp;当前的 `Bean` 是 `demoServiceTwo` 注入的对象是 `demoServiceThree`。结合这个可以对上面的 `Map` 有一个更直观的理解。

&ensp;&ensp;最后提醒一点，这两个 `Map` 中保存容器中所有对象之间的关系，直到容器被销毁的时候删除掉。


### 5. @Autowired 注解构造器的处理方式

&ensp;&ensp;前面介绍过，通过 `@Autowired` 注解构造器的方式，在 `populateBean()` 方法中，通过后置处理器来处理时，获取到的被注入的元素为空，因此直接返回。也就是说这里并没有维护对象之间的依赖关系。但是对象和属性之间的依赖关系，在通过构造器实例化对象的时候已经依赖好了。我自己的理解就是 `java` 对象和对象属性之间的关系已经有了。


### 6. 总结
&ensp;&ensp;本文主要介绍了 Spring 中对象之间依赖关系的处理流程。通过流程图的方式，粗略的看了一下 `@Resource` 和 `@Autowired` 注解处理的过程。

&ensp;&ensp;本文详细介绍了 `@Autowired` 注解属性的处理过程、`java` 对象与属性关系的维护以及 `Spring` 对象之间的依赖关系的维护。

&ensp;&ensp;简单介绍了 `@Autowired` 注解构造器的处理构成。

&ensp;&ensp;关于 `@Resource` 注解与 `@Autowired` 注解方法的处理过程，后面有机会在详细分析。









