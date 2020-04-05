## Spring依赖注入之createBean()
### MergedBeanDefinitionPostProcessor 的应用
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




