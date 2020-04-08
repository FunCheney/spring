## createBeanInstance() 通过构造器的方式注入对象

### 通过构造器的方式注入对象

```java
@Service
public class MyService {

	private Logger log = LoggerFactory.getLogger(MyService.class);

	MyTest myTest;

	@Autowired
	public MyService(MyTest myTest){
		this.myTest = myTest;
	}
	public void test(){
		System.out.println("hello test");
		myTest.test();
	}
}
```

&ensp;&ensp;通过构造器的方式注入对象在 `AbstractAutowireCapableBeanFactory` 类中的
`createBeanInstance()`方法中的代码片段如下：

```java
/**
 * 使用构造函数进行实例化
 * 由后置处理器决定返回那些构造方法
 */
Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
        mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
    return autowireConstructor(beanName, mbd, ctors, args);
}
```
&ensp;&ensp;`autowireConstructor()`方法的实现中最后调用的是 `ConstructorResolver`中的
`autowireConstructor()`方法。
```
protected BeanWrapper autowireConstructor(
        String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

    return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
}
```

&ensp;&ensp;`autowireConstructor`方法的实现复杂，下面就来分析一下该方法的实现与所完成的功能。
```java
public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
			@Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

		// 实例化一个BeanWrapperImpl对象
		BeanWrapperImpl bw = new BeanWrapperImpl();

		this.beanFactory.initBeanWrapper(bw);

		/**
		 * 确定参数列表，第一种通过BeanDefinition 设置
		 * 也可以通过 xml设置
		 * constructorToUse spring 决定采用哪个构造方法初始化
		 */
		Constructor<?> constructorToUse = null;
		ArgumentsHolder argsHolderToUse = null;
		Object[] argsToUse = null;

		if (explicitArgs != null) {
			argsToUse = explicitArgs;
		}
		else {
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				//获取已解析的构造方法
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					// Found a cached constructor...
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			if (argsToResolve != null) {
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve, true);
			}
		}

		// 没有已解析的构造方法
		if (constructorToUse == null || argsToUse == null) {
			// 解析构造方法
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				Class<?> beanClass = mbd.getBeanClass();
				try {
					candidates = (mbd.isNonPublicAccessAllowed() ?
							beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Resolution of declared constructors on bean Class [" + beanClass.getName() +
							"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
				}
			}

			if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
				Constructor<?> uniqueCandidate = candidates[0];
				if (uniqueCandidate.getParameterCount() == 0) {
					synchronized (mbd.constructorArgumentLock) {
						mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
						mbd.constructorArgumentsResolved = true;
						mbd.resolvedConstructorArguments = EMPTY_ARGS;
					}
					bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
					return bw;
				}
			}

			// Need to resolve the constructor.
			// 判断构造方法是否为空，判断是否根据构造方法自动注入
			boolean autowiring = (chosenCtors != null ||
					mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			ConstructorArgumentValues resolvedValues = null;

			/**
			 * 定义了最小参数个数
			 * 如果给构造方法的参数列表给定了具体的值
			 * 那么这些值的个数就是构造方法参数的个数
			 */
			int minNrOfArgs;
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			}
			else {
				/**
				 * 实例化一个对象，用来存放构造方法的参数值
				 * 主要存放参数值和参数值对应的下标
				 */
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				resolvedValues = new ConstructorArgumentValues();
				// 确定构造方法的参数数量
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
			}

			//排序
			/**
			 * 优先访问权限，访问权限相同，
			 * 通过构造器的参数个数来排序
			 */
			AutowireUtils.sortConstructors(candidates);

			//定义了一个差异变量
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Set<Constructor<?>> ambiguousConstructors = null;
			LinkedList<UnsatisfiedDependencyException> causes = null;

			// 循环所有的构造方法
			for (Constructor<?> candidate : candidates) {
				Class<?>[] paramTypes = candidate.getParameterTypes();

				/**
				 * 判断是否确定的具体的构造方法来完成实例化
				 * argsToUse.length > paramTypes.length
				 *
				 */
				if (constructorToUse != null && argsToUse != null && argsToUse.length > paramTypes.length) {
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					break;
				}
				if (paramTypes.length < minNrOfArgs) {
					continue;
				}

				ArgumentsHolder argsHolder;
				if (resolvedValues != null) {
					try {
						/**
						 * 判断是否加了ConstructorProperties注解，如果加了，则把值取出来
						 */
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
						if (paramNames == null) {
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								// 获取构造参数列表
								paramNames = pnd.getParameterNames(candidate);
							}
						}
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
						}
						// Swallow and try next constructor.
						if (causes == null) {
							causes = new LinkedList<>();
						}
						causes.add(ex);
						continue;
					}
				}
				else {
					// Explicit arguments given -> arguments length must match exactly.
					if (paramTypes.length != explicitArgs.length) {
						continue;
					}
					argsHolder = new ArgumentsHolder(explicitArgs);
				}

				int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				// Choose this constructor if it represents the closest match.
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					minTypeDiffWeight = typeDiffWeight;
					ambiguousConstructors = null;
				}
				else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					if (ambiguousConstructors == null) {
                    ambiguousConstructors = new LinkedHashSet<>();
                    ambiguousConstructors.add(constructorToUse);
                }
                ambiguousConstructors.add(candidate);
            }
        }

        if (constructorToUse == null) {
            if (causes != null) {
                UnsatisfiedDependencyException ex = causes.removeLast();
                for (Exception cause : causes) {
                    this.beanFactory.onSuppressedException(cause);
                }
                throw ex;
            }
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Could not resolve matching constructor " +
                    "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
        }
        else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Ambiguous constructor matches found in bean '" + beanName + "' " +
                    "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                    ambiguousConstructors);
        }

        if (explicitArgs == null && argsHolderToUse != null) {
            argsHolderToUse.storeCache(mbd, constructorToUse);
        }
    }

    Assert.state(argsToUse != null, "Unresolved constructor arguments");
    bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
    return bw;
}
```
&ensp;&ensp;上述方法，主要通过以下几个部分的考虑来完成代码逻辑的实现，一一做以总结：

①：构造函数的参数的确定

+ 根据explicitArgs 参数判断

&ensp;&ensp;如果传入的参数 explicitArgs 不为空，那么可以直接确定参数，应为
explicitArgs 参数是在条用 Bean 的时候用户指定的，在 BeanFactory
类中存在这样的方法：
>    Object getBean(String name, Object... args) throws BeansException;

&ensp;&ensp;在获取 bean 的时候，用户不但可以指定 bean 的名称还可以指定 bean 所对应类的构造
函数或者工厂方法的方法参数，主要用于静态工厂的调用，而这里是需要给定完全匹配的参数的，所以，便可以判断，
如果传入参数 explicitArgs 不为空，则可以确定构造函数参数就是它。

+ 缓存中获取

&ensp;&ensp;初次之外，确定参数的办法如果之前以及分析过，也就是说构造函数参数已经记录在缓存中，那么便可
以直接拿来使用。而且，这里要提到的是，在缓存中缓存的可能是参数的最终类型也可能是参数最初的类型，例如：构造函数
参数要求的int类型，但是原始的参数值可能是String类型的，那么即使在缓存中得到了参数，也需要经过类型转换器的过滤
以确保参数类型与对应的构造函数参数类型完全对应。

+ 配置文件中获取

&ensp;&ensp;如果不能根据传入的参数 explicitArgs 确定构造函数的参数也无法在缓存中得到相关信息。那么只能
开始新一轮的分析了。

&ensp;&ensp;分析从配置文件中配置的构造函数信息开始，经过之前的分析，我们知道，Spring中配置文件中的信息经过
转换都会通过 BeanDefinition 来承载，也就是参数 mbd 中包含，那么可以通过 `mbd.getConstructorArgumentValues()`
来获取对应的参数信息了，获取参数值的信息包括直接指定值，如：直接指定构造函数中某个值为原始类型 String 类型，或者是一个对其他
bean 的引用，而这一处理委托给 resolveConstructorArguments 方法，并返回能解析到的参数个数。

②：构造函数的确定

&ensp;&ensp;根据构造函数参数在所有构造函数中锁定对应的构造函数，匹配的方法，就是根据参数的个数匹配，所以在匹配之前需要
先对构造函数按照public构造函数优先参数数量降序、非public构造函数参数数量降序。这样可以在遍历的情况下迅速的判断在后面的
构造函数参数个数是否符合条件。

③：根据确定的构造函数转换对应的类型参数

&ensp;&ensp;主要使用Spring中提供的类型转换器或用户自定义的类型转换器进行转换。

④：构造函数不确定性的验证


⑤：根据实例化策略以及得到的构造参数及构造参数实例化 Bean。








