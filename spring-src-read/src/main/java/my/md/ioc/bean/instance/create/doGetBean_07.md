### 确定构造方法创建实例
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
                // 最小差异值复值
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