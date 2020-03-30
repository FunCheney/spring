## `doGetBean()`方法涉及的子过程解析

### 1.原型模式的依赖检查

`AbstractBeanFactory#isPrototypeCurrentlyInCreation(String
beanName)`方法

```java
protected boolean isPrototypeCurrentlyInCreation(String beanName) {
    Object curVal = this.prototypesCurrentlyInCreation.get();
    return (curVal != null &&
            (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
}
```
&ensp;&ensp;在`doGetBean()`方法中，如果当前正在创建的类是 prototype
类型的则抛出异常，异常信息 `Error creating bean with name `
【beanName】`Requested bean is currently in creation: Is there an
unresolvable circular reference?` 因为后面的代码涉及循环依赖的处理过程，由此可知
prototype 类型的bean
不做循环依赖的处理，只有在单例的情况下，spring才会尝试去解决循环依赖。

### 2.检测parentBeanFactory
整个过程较为简单，都是委托 parentBeanFactory 的 getBean() 进行处理，只不过在获取之
前对 name 进行简单的处理，主要是想获取原始的 beanName。

```java
protected String originalBeanName(String name) {
    // 是对 name 进行转换，获取真正的 beanName
    String beanName = transformedBeanName(name);
    // 判断name 是不是 FactoryBean, 以 "&" 开始
    if (name.startsWith(FACTORY_BEAN_PREFIX)) {
        // 将 beanName 还原成 FactoryBean 的形式
        beanName = FACTORY_BEAN_PREFIX + beanName;
    }
    return beanName;
}
```

### 3.类型检查

```java
protected void markBeanAsCreated(String beanName) {
    // 没有创建
    if (!this.alreadyCreated.contains(beanName)) {
        // 通过 synchronized 保证只有一个线程创建
        synchronized (this.mergedBeanDefinitions) {
            // 再次检查 没有创建
            if (!this.alreadyCreated.contains(beanName)) {
                // 从 mergedBeanDefinitions 中删除 beanName，
                // 并在下次访问时重新创建它
                clearMergedBeanDefinition(beanName);
                // 添加到已创建bean 集合中
                this.alreadyCreated.add(beanName);
            }
        }
    }
}
```
&ensp;&ensp;参数 typeCheckOnly 是用来判断调用 getBean() 是否为类型检查获取
bean。如果不是仅仅做类型检查则是创建bean，则需要调用 markBeanAsCreated() 记录.


### 4.BeanDefinition 的合并

```java
protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
    // 从缓存中获取，如果不为空，则返回
    RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
    if (mbd != null) {
        return mbd;
    }
    /*
     * getBeanDefinition(beanName)  获取 RootBeanDefinition
     * 如果返回的 BeanDefinition 是子类 bean 的话，则合并父类相关属性
     */
    return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
}
```
```java
protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
        throws BeanDefinitionStoreException {

    return getMergedBeanDefinition(beanName, bd, null);
}
```
```java
protected RootBeanDefinition getMergedBeanDefinition(
        String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
        throws BeanDefinitionStoreException {

    synchronized (this.mergedBeanDefinitions) {
        // 准备一个RootBeanDefinition变量引用，用于记录要构建和最终要返回的BeanDefinition.
        RootBeanDefinition mbd = null;

        // Check with full lock now in order to enforce the same merged instance.
        if (containingBd == null) {
            mbd = this.mergedBeanDefinitions.get(beanName);
        }

        if (mbd == null) {
            if (bd.getParentName() == null) {
                // bd不是一个ChildBeanDefinition的情况,换句话讲，这 bd应该是 :
                // 1. 一个独立的 GenericBeanDefinition 实例，parentName 属性为null
                // 2. 或者是一个 RootBeanDefinition 实例，parentName 属性为null
                // 此时mbd直接使用一个bd的复制品
                if (bd instanceof RootBeanDefinition) {
                    mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
                }
                else {
                    mbd = new RootBeanDefinition(bd);
                }
            }
            else {
                // bd是一个ChildBeanDefinition的情况,
                // 这种情况下，需要将bd和其parent bean definition 合并到一起，
                // 形成最终的 mbd
                // 下面是获取bd的 parent bean definition 的过程，最终结果记录到 pbd，
                // 并且可以看到该过程中递归使用了getMergedBeanDefinition(), 为什么呢?
                // 因为 bd 的 parent bd 可能也是个ChildBeanDefinition，所以该过程
                // 需要递归处理
                BeanDefinition pbd;
                try {
                    String parentBeanName = transformedBeanName(bd.getParentName());
                    if (!beanName.equals(parentBeanName)) {
                        pbd = getMergedBeanDefinition(parentBeanName);
                    }
                    else {
                        BeanFactory parent = getParentBeanFactory();
                        if (parent instanceof ConfigurableBeanFactory) {
                            pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
                        }
                        else {
                            throw new NoSuchBeanDefinitionException(parentBeanName,
                                    "Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
                                    "': cannot be resolved without an AbstractBeanFactory parent");
                        }
                    }
                }
                catch (NoSuchBeanDefinitionException ex) {
                    throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
                            "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
                }
                // 现在已经获取 bd 的parent bd到pbd，从上面的过程可以看出，这个pbd
                // 也是已经"合并"过的。
                // 这里根据pbd创建最终的mbd，然后再使用bd覆盖一次，
                // 这样就相当于mbd来自两个BeanDefinition:
                // 当前 BeanDefinition 及其合并的("Merged")双亲 BeanDefinition,
                // 然后mbd就是针对当前bd的一个MergedBeanDefinition(合并的BeanDefinition)了。
                mbd = new RootBeanDefinition(pbd);
                mbd.overrideFrom(bd);
            }

            // Set default singleton scope, if not configured before.
            if (!StringUtils.hasLength(mbd.getScope())) {
                mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
            }

            // A bean contained in a non-singleton bean cannot be a singleton itself.
            // Let's correct this on the fly here, since this might be the result of
            // parent-child merging for the outer bean, in which case the original inner bean
            // definition will not have inherited the merged outer bean's singleton status.
            if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
                mbd.setScope(containingBd.getScope());
            }

            // Cache the merged bean definition for the time being
            // (it might still get re-merged later on in order to pick up metadata changes)
            if (containingBd == null && isCacheBeanMetadata()) {
                this.mergedBeanDefinitions.put(beanName, mbd);
            }
        }

        return mbd;
    }
}
```
&ensp;&ensp;获取到合并的 BeanDefinition 后，对其进行检查。

```java
protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args)
        throws BeanDefinitionStoreException {
    /**
     * 这里判断 mbd 是否是抽象的
     * 如果是抽象的抛出
     *   Error creating bean with name "beanName" Bean definition is abstract
     */
    if (mbd.isAbstract()) {
        //抛出异常
        throw new BeanIsAbstractException(beanName);
    }
}
```
### 4.依赖检查
isDependent() 是校验该依赖是否已经注册给当前 bean。

```java
protected boolean isDependent(String beanName, String dependentBeanName) {
    synchronized (this.dependentBeanMap) {
        return isDependent(beanName, dependentBeanName, null);
    }
}
```
同步加锁给 dependentBeanMap 对象，然后调用 isDependent() 校验。
```java
private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
    if (alreadySeen != null && alreadySeen.contains(beanName)) {
        return false;
    }
    // 获取规范的 beanName
    String canonicalName = canonicalName(beanName);
    // 获取当前 beanName 的依赖集合
    Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
    // 不存在依赖，返回false
    if (dependentBeans == null) {
        return false;
    }
    // 存在，则证明存在已经注册的依赖
    if (dependentBeans.contains(dependentBeanName)) {
        return true;
    }
    // 递归检测依赖
    for (String transitiveDependency : dependentBeans) {
        if (alreadySeen == null) {
            alreadySeen = new HashSet<>();
        }
        alreadySeen.add(beanName);
        if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
            return true;
        }
    }
    return false;
}
```
如果校验成功，则调用 registerDependentBean() 将该依赖进行注册。

```java
public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

    /*
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
在完成依赖关系的缓存之后，通过 `getBean()` 实例化依赖 bean。

**这里在介绍两个Map：**

```
/** 指定的bean与目前已经注册的依赖这个指定的bean的所有依赖关系的缓存（我依赖的）*/
private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

/** 指定bean与目前已经注册的创建这个bean所需依赖的所有bean的依赖关系的缓存（依赖我的) */
private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);

```

