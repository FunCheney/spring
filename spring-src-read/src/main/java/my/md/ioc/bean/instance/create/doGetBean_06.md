### 1.前情提要
&ensp;&ensp;文章开篇，不得不前情提要走一波了。还记得 `@Configuration` 类中的`@Bean`方法是如何处理的吗？小小的脑袋上面是否有大大的问号呢？
这里做一个简要回顾，首先看 `@Bean` 方法的处理：
@Bean方法处理_01.jpg

&ensp;&ensp;然后在通过下面以系列的方法对其解析：
@Bean方法解析概要.jpg

&ensp;&ensp;上述图片中的最后一步，是不是很亲切？是不是看到了熟悉的 `registerBeanDefinition()`方法？是不是还能想起 `this.beanDefinitionMap.put(beanName
, beanDefinition);`？

&ensp;&ensp;最后转化成相应的 `BeanDefinition` 注册到 `BeanDefinitionMap` 中去：
@Bean_02.jpg
&ensp;&ensp;首先我们其中一个 `BeanDefinition` 为例，看看这个 `BeanDefinition` 中都包含哪些信息。
 
&ensp;&ensp;这里看到，将 `BeanDefinition` 注册到 `Map` 中去了，但是这里这个注册并不是这么简单的。这里要对一下几种情况加以区分，要不然看到后面的
`instantiateUsingFactoryMethod()` 方法肯定会懵圈的。

&ensp;&ensp;上述代码中用到的两个类：
```java
public class DemoServiceOne {
	@Autowired
	DemoServiceTwo demoServiceTwo;
	public DemoServiceOne(){

	}

	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	}
}
```

```java
@Service
public class DemoServiceTwo {
}
```

```java
public class BeanDemoOne {
}
```

```java
public class FactoryBeanDemoOne implements FactoryBean<BeanDemoOne> {
	@Override
	public BeanDemoOne getObject() throws Exception {
		return new BeanDemoOne();
	}

	@Override
	public Class<?> getObjectType() {
		return null;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
	public FactoryBeanDemoOne (){}

	public FactoryBeanDemoOne (int i){

	}
}
```

#### 1.1 静态的 @Bean 方法

```java
@Configuration
@ComponentScan("com.demo")
public class DemoConfigOne {

	@Bean("demoService")
	public static DemoServiceOne demoServiceOne(){
		return new DemoServiceOne();
	}
}
```
situation_01.jpg
#### 1.2 非静态的构造方法
```java
@Configuration
@ComponentScan("com.demo")
public class DemoConfigOne {

	@Bean
	public FactoryBeanDemoOne demoFactoryBean() {
		return new FactoryBeanDemoOne();
	}
}
```
situation_02.jpg

#### 1.3 通过两个 @Bean 返回同类型的对象
```java
@Configuration
@ComponentScan("com.demo")
public class DemoConfigOne {

	    @Bean
    	public  DemoServiceOne demoServiceOne(DemoServiceTwo demoServiceTwo){
    		return new DemoServiceOne(demoServiceTwo);
    	}
    
    	@Bean
    	public  DemoServiceOne demoServiceOne(){
    		return new DemoServiceOne();
    	}

}
```
situation_03.jpg
#### 1.4 通过两个 @Bean 返回指定名称的同类型对象 
```java
@Configuration
@ComponentScan("com.demo")
public class DemoConfigOne {

    @Bean("demoOne")
	public  DemoServiceOne demoServiceOne(DemoServiceTwo demoServiceTwo){
		return new DemoServiceOne(demoServiceTwo);
	}

	@Bean("demoTwo")
	public  DemoServiceOne demoServiceOne(){
		return new DemoServiceOne();
	}

}
```
situation_04.jpg

### 2.通过工厂方法创建实例
&ensp;&ensp;在不禁感慨Spring的强大的同时，在看到某些方法的时候，也觉得某些方法好像不是那么符合Spring的设计？下面这个方法，就是写的比较绕的方法，
没有办法，只能硬着头皮往下看。。。

```java
public BeanWrapper instantiateUsingFactoryMethod(
        String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

    // 构造 BeanWrapperImpl 对象
    BeanWrapperImpl bw = new BeanWrapperImpl();
    /*
     * 初始化 BeanWrapperImpl
     * 向BeanWrapper对象中添加 ConversionService 对象和属性编辑器 PropertyEditor 对象
     */
    this.beanFactory.initBeanWrapper(bw);

    Object factoryBean;
    Class<?> factoryClass;
    boolean isStatic;

    // 通过beanDefinition获取到factoryBeanName ，实际就是@Bean注解的方法 所在的 configuration类
    String factoryBeanName = mbd.getFactoryBeanName();
    if (factoryBeanName != null) {
        // factoryBeanName 与 当前的 beanName 相同 抛出异常
        if (factoryBeanName.equals(beanName)) {
            throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                    "factory-bean reference points back to the same bean definition");
        }
        // 根据 BeanName 获取 对象，就是 @Configuration 注解的类 这里获取到的是被 CGLIB 代理的类
        factoryBean = this.beanFactory.getBean(factoryBeanName);
        if (mbd.isSingleton() && this.beanFactory.containsSingleton(beanName)) {
            throw new ImplicitlyAppearedSingletonException();
        }
        // 获取工厂类
        factoryClass = factoryBean.getClass();
        isStatic = false;
    }
    else {
        // 工厂名称为空，则可能是一个静态工厂
        // 如果有static 且为工厂方法，则添加到 candidateList 中
        // 这加这个判断是以防漏掉 加了 static 的 @Bean 方法。当然，没有加 @Bean 的方法就不会被考虑了
        if (!mbd.hasBeanClass()) {
            throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                    "bean definition declares neither a bean class nor a factory-bean reference");
        }
        factoryBean = null;
        factoryClass = mbd.getBeanClass();
        // 标记为静态属性
        isStatic = true;
    }

    // 工厂方法
    Method factoryMethodToUse = null;
    // 持有的参数
    ArgumentsHolder argsHolderToUse = null;
    // 使用的参数
    Object[] argsToUse = null;

    /*
     * 工厂方法的参数，如果指定了构造参数，则直接使用
     * @Bean注解的方法（工厂方法）的参数，在启动过程中实例化的对象 这里一般都为null，即一般不指定参数
     * 追溯来源：就是 getBean() 方法中的 args 为null
     */
    if (explicitArgs != null) {
        argsToUse = explicitArgs;
    }
    else {
        // 没有指定
        Object[] argsToResolve = null;
        synchronized (mbd.constructorArgumentLock) {
            // 首先尝试从缓存中获取
            factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod;
            // 获取缓存中的构造函数或者工厂方法，不为空表示已经使用过工厂方法，那么这里会再次使用
            // 一般原型模式和Scope模式采用的上，直接使用该工厂方法和缓存的参数
            if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) {
                // 获取缓存中的构造参数
                argsToUse = mbd.resolvedConstructorArguments;
                if (argsToUse == null) {
                    argsToResolve = mbd.preparedConstructorArguments;
                }
            }
        }
        // 缓存中存在,则解析存储在 BeanDefinition 中的参数
        // 如给定方法的构造函数 A(int ,int )，则通过此方法后就会把配置文件中的("1","1")转换为 (1,1)
        // 缓存中的值可能是原始值也有可能是最终值
        if (argsToResolve != null) {
            argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve, true);
        }
    }

    // getBean() 方法没有传参数 或 没有使用过 工厂方法
    if (factoryMethodToUse == null || argsToUse == null) {
        // 获取工厂方法的类全名称
        factoryClass = ClassUtils.getUserClass(factoryClass);

        // 获取所有声明的构造方法，默认允许访问非公开的方法
        Method[] rawCandidates = getCandidateMethods(factoryClass, mbd);
        // 检索所有方法，这里是对方法进行过滤
        List<Method> candidateList = new ArrayList<>();
        for (Method candidate : rawCandidates) {
            if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
                // 添加到候选类里面去
                candidateList.add(candidate);
            }
        }

        /**
         * candidateList.size() == 1 表示待定的方法只有一个
         * explicitArgs == null 调用getBean方法时没有传参
         * !mbd.hasConstructorArgumentValues() 没有缓存过参数，
         *     直接通过调用实例化方法执行该候选方法
         */
        if (candidateList.size() == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
            Method uniqueCandidate = candidateList.get(0);
            if (uniqueCandidate.getParameterCount() == 0) {
                mbd.factoryMethodToIntrospect = uniqueCandidate;
                synchronized (mbd.constructorArgumentLock) {
                    mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
                    mbd.constructorArgumentsResolved = true;
                    mbd.resolvedConstructorArguments = EMPTY_ARGS;
                }
                bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, uniqueCandidate, EMPTY_ARGS));
                // 返回
                return bw;
            }
        }

        Method[] candidates = candidateList.toArray(new Method[0]);
        // 排序构造函数
        // public 构造函数优先参数数量降序，非public 构造函数参数数量降序
        AutowireUtils.sortFactoryMethods(candidates);

        // 用于承载解析后的构造函数参数的值
        ConstructorArgumentValues resolvedValues = null;
        boolean autowiring = (mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
        // 初始化最小差异变量
        int minTypeDiffWeight = Integer.MAX_VALUE;
        Set<Method> ambiguousFactoryMethods = null;
        // 初始化最小的参数个数
        int minNrOfArgs;
        // 如果调用getBean方法时有传参，那么工厂方法最少参数个数要等于传参个数
        if (explicitArgs != null) {
            minNrOfArgs = explicitArgs.length;
        }
        else {
            // getBean() 没有传递参数，则需要解析保存在 BeanDefinition 构造函数中指定的参数
            if (mbd.hasConstructorArgumentValues()) {
                // 构造函数的参数
                ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
                // 解析构造函数的参数
                // 将该 bean 的构造函数参数解析为 resolvedValues 对象，其中会涉及到其他 bean
                resolvedValues = new ConstructorArgumentValues();
                // 解析构造函数的参数 返回对应的参数个数 赋值给最小参数个数变量
                minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
            }
            else {
                minNrOfArgs = 0;
            }
        }

        LinkedList<UnsatisfiedDependencyException> causes = null;

        // 遍历候选方法 （这里拿到的其实就是实例化 Bean 的构造方法）
        for (Method candidate : candidates) {
            // 方法的参数列表
            Class<?>[] paramTypes = candidate.getParameterTypes();

            if (paramTypes.length >= minNrOfArgs) {
                // 保存参数的对象
                ArgumentsHolder argsHolder;

                // getBean()传递了参数
                if (explicitArgs != null) {
                    // 显示给定参数，参数长度必须完全匹配
                    if (paramTypes.length != explicitArgs.length) {
                        continue;
                    }
                    // 根据参数创建参数持有者
                    argsHolder = new ArgumentsHolder(explicitArgs);
                }
                else {
                    // 未提供参数，解析构造参数
                    try {
                        String[] paramNames = null;
                        // 获取 ParameterNameDiscoverer 对象
                        // ParameterNameDiscoverer 是用于解析方法和构造函数的参数名称的接口，为参数名称探测器
                        ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                        if (pnd != null) {
                            // 获取指定构造函数的参数名称
                            paramNames = pnd.getParameterNames(candidate);
                        }
                        // 在已经解析的构造函数参数值的情况下，创建一个参数持有者对象
                        argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw,
                                paramTypes, paramNames, candidate, autowiring, candidates.length == 1);
                    }
                    catch (UnsatisfiedDependencyException ex) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Ignoring factory method [" + candidate + "] of bean '" + beanName + "': " + ex);
                        }
                        // Swallow and try next overloaded factory method.
                        if (causes == null) {
                            causes = new LinkedList<>();
                        }
                        causes.add(ex);
                        continue;
                    }
                }

                // isLenientConstructorResolution 判断解析构造函数的时候是否以宽松模式还是严格模式 默认宽松模式
                // 严格模式：解析构造函数时，必须所有的都需要匹配，否则抛出异常
                // 宽松模式：使用具有"最接近的模式"进行匹配
                // typeDiffWeight：类型差异权重
                int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
                        argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
                // 代表最接近的类型匹配，则选择作为构造函数
                if (typeDiffWeight < minTypeDiffWeight) {
                    factoryMethodToUse = candidate;
                    argsHolderToUse = argsHolder;
                    argsToUse = argsHolder.arguments;
                    minTypeDiffWeight = typeDiffWeight;
                    ambiguousFactoryMethods = null;
                }
                // 如果具有相同参数数量的方法具有相同的类型差异权重，则收集此类型选项
                // 但是，仅在非宽松构造函数解析模式下执行该检查，并显式忽略重写方法（具有相同的参数签名）
                else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&
                        !mbd.isLenientConstructorResolution() &&
                        paramTypes.length == factoryMethodToUse.getParameterCount() &&
                        !Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
                    // 查找到多个可匹配的方法
                    if (ambiguousFactoryMethods == null) {
                        ambiguousFactoryMethods = new LinkedHashSet<>();
                        ambiguousFactoryMethods.add(factoryMethodToUse);
                    }
                    ambiguousFactoryMethods.add(candidate);
                }
            }
        }

        // 没有可执行的工厂方法，抛出异常
        if (factoryMethodToUse == null) {
            if (causes != null) {
                UnsatisfiedDependencyException ex = causes.removeLast();
                for (Exception cause : causes) {
                    this.beanFactory.onSuppressedException(cause);
                }
                throw ex;
            }
            List<String> argTypes = new ArrayList<>(minNrOfArgs);
            if (explicitArgs != null) {
                for (Object arg : explicitArgs) {
                    argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
                }
            }
            else if (resolvedValues != null) {
                Set<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
                valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
                valueHolders.addAll(resolvedValues.getGenericArgumentValues());
                for (ValueHolder value : valueHolders) {
                    String argType = (value.getType() != null ? ClassUtils.getShortName(value.getType()) :
                            (value.getValue() != null ? value.getValue().getClass().getSimpleName() : "null"));
                    argTypes.add(argType);
                }
            }
            String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "No matching factory method found: " +
                    (mbd.getFactoryBeanName() != null ?
                        "factory bean '" + mbd.getFactoryBeanName() + "'; " : "") +
                    "factory method '" + mbd.getFactoryMethodName() + "(" + argDesc + ")'. " +
                    "Check that a method with the specified name " +
                    (minNrOfArgs > 0 ? "and arguments " : "") +
                    "exists and that it is " +
                    (isStatic ? "static" : "non-static") + ".");
        }
        //返回类型不能为void
        else if (void.class == factoryMethodToUse.getReturnType()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Invalid factory method '" + mbd.getFactoryMethodName() +
                    "': needs to have a non-void return type!");
        }
        //存在含糊的两个工厂方法，不知选哪个
        else if (ambiguousFactoryMethods != null) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Ambiguous factory method matches found in bean '" + beanName + "' " +
                    "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                    ambiguousFactoryMethods);
        }

        if (explicitArgs == null && argsHolderToUse != null) {
            mbd.factoryMethodToIntrospect = factoryMethodToUse;
            argsHolderToUse.storeCache(mbd, factoryMethodToUse);
        }
    }

    Assert.state(argsToUse != null, "Unresolved factory method arguments");
    // 实例化 并返回
    bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, factoryMethodToUse, argsToUse));
    return bw;
}
```
#### 2.1 方法流程图

#### 2.2 方法分析
&ensp;&ensp;首先这个方法的流程比较长，首先声明：关于推断构造函数的使用，这里暂时不做

```java
public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
    // If valid arguments found, determine type difference weight.
    // Try type difference weight on both the converted arguments and
    // the raw arguments. If the raw weight is better, use it.
    // Decrease raw weight by 1024 to prefer it over equal converted weight.
    // 与转换之前的参数作比较，确定一个差异值
    int typeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.arguments);
    // 与转换之后的参数作比较，确定一个差异值
    int rawTypeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
    return (rawTypeDiffWeight < typeDiffWeight ? rawTypeDiffWeight : typeDiffWeight);
}
```
```java
public int getAssignabilityWeight(Class<?>[] paramTypes) {
    for (int i = 0; i < paramTypes.length; i++) {
        // 首先判断参数为转换之前的
        if (!ClassUtils.isAssignableValue(paramTypes[i], this.arguments[i])) {
            // 只要有一个参数类型不匹配 就返回 最大的权重值
            return Integer.MAX_VALUE;
        }
    }
    // 与转换之后的参数类型匹配
    for (int i = 0; i < paramTypes.length; i++) {
        if (!ClassUtils.isAssignableValue(paramTypes[i], this.rawArguments[i])) {
            // 有一个参数不匹配 就用最大的权重值减去 512
            return Integer.MAX_VALUE - 512;
        }
    }
    // 到这里 就是最匹配的，可以看出最小匹配值是 最大匹配值 - 1024。可以看出，权重匹配值的范围 就是 0 ~ 1024
    return Integer.MAX_VALUE - 1024;
}
```

```java
public static int getTypeDifferenceWeight(Class<?>[] paramTypes, Object[] args) {
    int result = 0;
    for (int i = 0; i < paramTypes.length; i++) {
        if (!ClassUtils.isAssignableValue(paramTypes[i], args[i])) {
            // 只要有一个双女户类型不匹配，返回最大权重值
            return Integer.MAX_VALUE;
        }
        if (args[i] != null) {
            Class<?> paramType = paramTypes[i];
            Class<?> superClass = args[i].getClass().getSuperclass();
            // 父类不为null
            while (superClass != null) {
                // 注入参数的类型 是方法参数类型的子类，每往上找一层子类
                // 差异值 +2，一直找到与方法参数的类型相同
                if (paramType.equals(superClass)) {
                    result = result + 2;
                    superClass = null;
                }
                else if (ClassUtils.isAssignable(paramType, superClass)) {
                    result = result + 2;
                    superClass = superClass.getSuperclass();
                }
                else {
                    superClass = null;
                }
            }
            // 方法参数类型是接口 +1
            if (paramType.isInterface()) {
                result = result + 1;
            }
        }
    }
    return result;
}
```





 