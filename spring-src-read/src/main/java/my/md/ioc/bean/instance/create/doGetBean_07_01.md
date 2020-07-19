## 找到更合适的构造器

### 1.没有最合适的，只有更合适的

&ensp;&ensp;上一篇文章中，我们说到了Spring确定有哪些构造器他可以使用，这一篇文章中，我们将来分析Spring是如何找到一个最合适的构造器。
```java
@Service
public class DemoServiceOne {

	DemoServiceTwo demoServiceTwo;

	DemoServiceThree demoServiceThree;

	public DemoServiceOne(){

	}

	@Autowired(required = false)
	public DemoServiceOne(DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
	}
	@Autowired(required = false)
	public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
		this.demoServiceTwo = demoServiceTwo;
		this.demoServiceThree = demoServiceThree;
	}
}
```
&ensp;&ensp;以上面这个类为例，Spring可以使用的构造器，最终确定为三个，如下图：

![确定可用的构造器](https://imgkr.cn-bj.ufileos.com/8d5dc227-11dc-40fe-8f99-bb98178cf1b5.jpg)
&ensp;&ensp;这里简单说明一下，图片中构造器的确定是上一篇文章分析的内容，如有疑问可查看：[如何确定构造器](https://juejin.im/post/5f0f25286fb9a07e753c9d40)
这里通过下图简单说明，这几个构造器是如何添加到`ctors`中的：
![确定构造器_02](https://imgkr.cn-bj.ufileos.com/80cbc947-a100-4291-8b21-df646d6e8311.jpg)


&ensp;&ensp;接下来就是本文的重点，通过构造器来完成对象的实例化。再有多个构造器的时候，是如何选出最合适的一个呢？


### 2.通过构造器来完成对象的实例化

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
    // 定义构造方法要使用那些参数
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
            // 这里 通过 new ConstructorArgumentValues() 的方式来实例化一个空对象
            resolvedValues = new ConstructorArgumentValues();
            // 确定构造方法的参数数量
            // 在通过 Spring 内部给了一个值的情况下 表示构造方法的最小参数个数
            // 在没有给的情况下 为 0
//				mbd.getConstructorArgumentValues().addGenericArgumentValue("");
            minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
        }

        //排序
        /**
         * 优先访问权限，访问权限相同，
         * 通过构造器的参数个数来排序
         */
        AutowireUtils.sortConstructors(candidates);

        //定义了一个最小类型差异变量
        int minTypeDiffWeight = Integer.MAX_VALUE;
        // 有歧义的构造方法
        Set<Constructor<?>> ambiguousConstructors = null;
        LinkedList<UnsatisfiedDependencyException> causes = null;

        // 循环所有的构造方法
        for (Constructor<?> candidate : candidates) {
            Class<?>[] paramTypes = candidate.getParameterTypes();

            /**
             * 判断是否确定的具体的构造方法来完成实例化
             * argsToUse.length > paramTypes.length
             * constructorToUse != null 表示已经有确定的构造方法来
             * argsToUse.length > paramTypes.length 说明要使用的参数与指定构造方法的参数个数不匹配 break 结束循环
             *
             */
            if (constructorToUse != null && argsToUse != null && argsToUse.length > paramTypes.length) {
                // Already found greedy constructor that can be satisfied ->
                // do not look any further, there are only less greedy constructors left.
                break;
            }
            // 当前构造方法使用的参数类型的个数，小于给定的构造方法的参数个数，
            // 说明当前的构造方法不匹配，结束本次循环，继续下一次
            if (paramTypes.length < minNrOfArgs) {
                continue;
            }

            ArgumentsHolder argsHolder;
            /**
             * 这里 resolvedValues 必不为空
             * 因为 在前面的处理过程中 explicitArgs 为null
             * 所以 resolvedValues 通过 new ConstructorArgumentValues() 方式完成初始化
             */
            if (resolvedValues != null) {
                try {
                    /**
                     * 判断是否加了ConstructorProperties注解，如果加了，则把值取出来
                     */
                    String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
                    if (paramNames == null) {
                        ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                        if (pnd != null) {
                            // 获取构造方法的 参数列表
                            paramNames = pnd.getParameterNames(candidate);
                        }
                    }
                    /**
                     * Spring 内部只提供字符串的参数值,故而需要转换
                     * argsHolder 所包含的值就是转换之后的
                     */
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

            /**
             * 定义了一个 类型差异量 typeDiffWeight： 这里要注意
             *  isLenientConstructorResolution 默认为true
             *  这行代码的逻辑就是要确定一个差异值
             */
            int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
                    argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
            // Choose this constructor if it represents the closest match.
            /**
             * 下面代码是确定一个一个最匹配的构造函数来
             * 首先：
             *    minTypeDiffWeight 是一个很大的值 typeDiffWeight 是一个负数，
             *        这也就是 Spring 前面为构造方法排序的原因，其实默认，参数最多的构造 public 构造方法是最合适的
             *    确保当前的 if 分支能够进入
             *        在if 分支中 给要使用的构造方法，以及要使用的参数做了赋值
             *        其中 minTypeDiffWeight = typeDiffWeight 就是用来判断哪个构造方法更合适，但是这样一来就会有一个问题,
             *        下一次的 for 循环进入到这里，不满足 if 分支判断条件 进入的 else if 中
             *    当进入到 else if 中
             *        说明有两个构造函数比较合适，这个时候 spring 就不知道选哪个构造函数来完成实例了
             */
            if (typeDiffWeight < minTypeDiffWeight) {
                constructorToUse = candidate;
                argsHolderToUse = argsHolder;
                argsToUse = argsHolder.arguments;
                // 最小差异值赋值
                minTypeDiffWeight = typeDiffWeight;
                // 重置有歧义的构造方法记录
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
&ensp;&ensp;上述方法的过程分析复杂，而Spring为了兼容开发人员在使用框架的过程中可能会发生的各种场景，所以做了很多的判断与处理。处理的场景多了
起来对应的异常情况也就多类起来。关于这个方法的分析，我试着画了一个流程图处理，我打算从流程图入手，试着梳理一下，如有错误之处，还请多多包涵！

#### 2.1 流程图
![确定更合适的构造器流程图](https://imgkr.cn-bj.ufileos.com/e004b1af-fcca-486a-af7d-e6a538f56112.png)

&ensp;&ensp;上述流程图中蓝颜色箭头标出的为主线流程，也是在日常开发中用到构造注入的大多数处理逻辑。在这里分析这个流程图，我准备拆分成两部分，第一部分就是蓝颜色线条整个的流程；第二部分就是用绿颜色线条圈出来的部分，也是Spring中用来确定合适的构造器的部分。我记得，我在 [factory-method实例化对象的文章](https://juejin.im/post/5f098531e51d4534661e1214)中**推断构造器**的部分说到后面会分析，这里就是。

#### 2.2 上述方法主流程
* 0.初始化参数

```java
     // 表示最终要使用的构造器
     Constructor<?> constructorToUse = null; 
     // 表示最终构造器要使用的参数
     Object[] argsToUse = null;
```   
1. `explicitArgs != null` 为`false`

&ensp;&ensp;在这一步中会进行一些判断，这些判断中最终的目的就是为了初始化 `constructorToUse` 与 `argsToUse`。遗憾的是大多数的情况下这一部分的判断逻辑最后得到的结果依然是这两参数为 `null`。

2. `constructorToUse == null || argsToUse == null` 为 `true`

  + `Constructor<?>[] candidates = chosenCtors;` 将上一篇文章中确定的构造器赋于 `candidates`;
  + `candidates == null `我自己的理解是一种容错机制，加入获取到构造器为 `null`,Spring中会再次获取一次。


3. 当确定的构造器只有一个的时候
    + `@Autowired`注入的是无参构造器完成初始化，方法结束
    ![注入无参数的构造器](https://imgkr.cn-bj.ufileos.com/f7d537fc-95b0-4dfb-b8bc-9233c797c370.jpg)
    + `@Autowired`注入的是有参数的构造器，方法流程继续
    ![注入有参数的构造器](https://imgkr.cn-bj.ufileos.com/86595e9e-e1e1-4ab5-be99-efa81c154acc.jpg)
    
4.判断是否根据构造方法注入，`@Autowired`注入构造器的方式，都是 `true`。
![自定注入标志](https://imgkr.cn-bj.ufileos.com/341e3442-c30c-41b8-bcca-e8b5a45662e0.jpg)

5.确定最小参数个数即 `minNrOfArgs` 的值，大多数情况下都为0。
  > 如果在程序中通过 mbd.getConstructorArgumentValues().addGenericArgumentValue(""); 的方式设置，那么这里获取到的就不是0。很少用到。
6. `AutowireUtils.sortConstructors(candidates);`对构造函数排序
  + 示例代码：
  ![代码](https://imgkr.cn-bj.ufileos.com/a3680cf9-3a75-4b41-9379-ae55a5c01810.jpg)
  + 排序之前：
  ![排序前](https://imgkr.cn-bj.ufileos.com/ad46b65c-5445-405e-8751-56ca37849d78.jpg)
  + 排序之后：
  ![排序之后](https://imgkr.cn-bj.ufileos.com/ba7e7a5d-918e-4aff-8a17-8029f4e768d7.jpg)
7.定义参数用于确定构造函数
```java
  //定义了一个最小类型差异变量
  int minTypeDiffWeight = Integer.MAX_VALUE;
  // 有歧义的构造方法
  Set<Constructor<?>> ambiguousConstructors = null;
```
&ensp;&ensp;差异变量的默认值是一个极大值。有歧义的构造方法集合默认为 `null`。

8.`for`循环选出来的构造器

  + 判断是否已经有确定的具体的构造方法来完成初始化，如果有则结束循环；一般不会结束循环;
  + 判断当前构造方法使用的参数类型的个数与前面确定的**最小参数个数**的大小。如小于说明不匹配，则终止当前循环，继续下一次循环。

9.确定类型差异变量`typeDiffWeight`的值，通过 `typeDiffWeight` 与 最小类型差异变量`minTypeDiffWeight`的值来确定构造函数。
```java
int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
						argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
```
&ensp;&ensp;这里涉及到通过**宽松模式**与**严格模式**来确定类型差异变量的值。这两种模式这里不是重点，后面在来看。总之我们只需要知道，在这里Spring确定了一个值，来做比较。

&ensp;&ensp;通过**构造器**注入的方式默认采用的是宽松模式，这里以我使用的例子为例最终返回的是一个负数：**-1024**!
![类型差异变量](https://imgkr.cn-bj.ufileos.com/2bf7e645-0e8c-4fd9-82fa-1ae5cbd222de.jpg)  

*接下来就是找的更合适的构造器的时候了，请看下一小节...*
#### 2.3 推断构造器

&ensp;&ensp;推断构造器，这里的代码也就**15行**，但是这部分的逻辑的确值我我们分析一下，先看下图：
![推断构造函数](https://imgkr.cn-bj.ufileos.com/70ddf55c-e14b-41ba-a1fe-5915a83925e2.jpg)

* 首先，测试代码如下：
```java
  @Service
  public class DemoServiceOne {
      DemoServiceTwo demoServiceTwo;
      DemoServiceThree demoServiceThree;
      
      @Autowired(required = false)
      public DemoServiceOne(DemoServiceTwo demoServiceTwo){
        this.demoServiceTwo = demoServiceTwo;
      }
      
      @Autowired(required = false)
      public DemoServiceOne(DemoServiceThree demoServiceThree, DemoServiceTwo demoServiceTwo){
        this.demoServiceTwo = demoServiceTwo;
        this.demoServiceThree = demoServiceThree;
      }
  }

```
  1. 第一次循环，以本文测试方法为例 **`typeDiffWeight` = -1024**;**`minTypeDiffWeight` = 2147483647**。`if()`条件成立成立，做了下面几件事：
  + `constructorToUse = candidate`当前构造器(**public的且参数最多的**)
  + `argsToUse = argsHolder.arguments;` 最终使用的参数赋值
  + `minTypeDiffWeight = typeDiffWeight;` 重置最小差异变量，用于后面的判断
  + `ambiguousConstructors = null;` 重置有歧义的构造方法集合，当然第一次进入这里本身就是 `null`。
  
&ensp;&ensp;注意，这次循环排在最前面的是`public`且**参数最多**的构造器。
  
  2. 第二次`for`循环中，在上述测试代码的情形下，会进入下面的判断语句中，因此结束循环。
  ```java
  if (constructorToUse != null && argsToUse != null && argsToUse.length > paramTypes.length) {
					// Already found greedy constructor that can be satisfied ->
					// do not look any further, there are only less greedy constructors left.
					break;
				}
  ```

* 其次测试代码如下：**有两个相同类型的构造函数**，即：构造函数的参数个数相同，修饰符类型相同。
```java
  @Service
  public class DemoServiceOne {
      DemoServiceTwo demoServiceTwo;
      DemoServiceThree demoServiceThree;
      
      @Autowired(required = false)
      public DemoServiceOne(DemoServiceTwo demoServiceTwo){
        this.demoServiceTwo = demoServiceTwo;
      }
      
      @Autowired(required = false)
      public DemoServiceOne(DemoServiceThree demoServiceThree){
        this.demoServiceThree = demoServiceThree;
      }
  }
```
1. 第一次`for`循环与上面第一个测试例子是相同的处理逻辑，这里不在赘述。
2. 第二次循环，由于两个构造函数的参数个数相同，所以上述的 `if()`条件中`argsToUse.length > paramTypes.length`这个判断不成立，因此循环继续。如下图：
![同类型的构造器](https://imgkr.cn-bj.ufileos.com/67ee5c0b-ee1c-45e2-8451-c2793aba0423.jpg)
可以看出，在有歧义的构造器中，最终添加了两个构造器。若有其他的构造器，上述的`if()` 条件成立，因此循环终止。
3. 然后，Spring要对有有歧义的构造器列表做处理
![有歧义的构造器处理](https://imgkr.cn-bj.ufileos.com/6c12c2e8-fd61-42df-953d-931df465c330.jpg)
可以看出，在**严格模式**下，如果有两个可用的构造器，Spring会直接抛出异常。而在默认的情况下，Spring使用的是**宽松模式**，因此方法继续。但是这样一来，Spring到底会使用哪个构造器呢？答案就在上述的源码中，那就是**第一次循环**的构造器。因为第二次的循环，在`else if`没有对 `constructorToUse`进行处理。
![最终使用的构造器](https://imgkr.cn-bj.ufileos.com/abc6888e-555b-4fe2-a718-2f672dae6a2b.jpg)

### 3.关于严格/宽松 模式
&ensp;&ensp; ***首先这里需要指出：*** `@Configuration`类中的`@Bean`方法的处理是严格模式。

&ensp;&ensp;Spring这两种模式在本文中有两处用到：①确定类型差异变量；②：在有，有歧义的构造器时做相应的校验。在确定最终的构造器的时候，个人理解：宽松模式就是检查条件更为宽松，只要有符合的便放过。与之对应的严格模式则校验条件更为严格，只要不符合，则抛出异常。

&ensp;&ensp;下面主要来分析一下，两种不同模式下如何确定类型差异变量的。

#### 3.1宽松模式
```java
public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
			// 与转换之前的参数作比较，确定一个差异值
			int typeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.arguments);
			// 与转换之后的参数作比较，确定一个差异值
			int rawTypeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
			return (rawTypeDiffWeight < typeDiffWeight ? rawTypeDiffWeight : typeDiffWeight);
		}
```
&ensp;&ensp;通过`getTypeDifferenceWeight()`来确定类型差异变量。
```java
{
		int result = 0;
		for (int i = 0; i < paramTypes.length; i++) {
			if (!ClassUtils.isAssignableValue(paramTypes[i], args[i])) {
				// 只要有一个参数类型不匹配，返回最大权重值
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

#### 3.2严格模式
```
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
### 4.总结
&ensp;&ensp;本文主要目的是尝试着解释一下，Spring是如何推荐构造器的。主要是画出了确定更合适的构造器的**流程图**。

&ensp;&ensp;然后根据不同的 **确定的构造器**的场景，来分析了一下Spring中的处理逻辑。

&ensp;&ensp;最后就是之前一直提到的 **宽松模式**与**严格模式**(ps:这里对于这两中方式的理解不是很到位，以后如果有更好的理解在做补充)




  
  
  
  
  
  
  
  
  
  
  





