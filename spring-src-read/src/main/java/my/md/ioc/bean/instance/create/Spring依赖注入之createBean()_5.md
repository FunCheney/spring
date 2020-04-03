## Spring依赖注入之createBean()
### `createBeanInstance()`

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // Make sure bean class is actually resolved at this point.
    /** 确认需要实例化的bean */
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    /**
     * 检查一个类的访问权限，spring默认情况下，对于非public的类是允许访问的
     */
    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }

    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }

    /** 使用工厂方法对Bean进行实例化*/
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    /**
     * 多次构建同一个Bean时，可以使用shortcut
     */
    boolean resolved = false;
    boolean autowireNecessary = false;
    if (args == null) {
        synchronized (mbd.constructorArgumentLock) {
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                resolved = true;
                // 如果已经解析了构造方法的参数，就必须通过一个带参数的构造方法来实例
                autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }
    }
    if (resolved) {
        if (autowireNecessary) {
            // 通过构造方法自动装配的方式构造Bean对象
            return autowireConstructor(beanName, mbd, null, null);
        }
        else {
            // 通过默认的无参构造方法进行实例化
            return instantiateBean(beanName, mbd);
        }
    }

    // Candidate constructors for autowiring?
    /**
     * 使用构造函数进行实例化
     * 由后置处理器决定返回那些构造方法
     */
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
            mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // Preferred constructors for default construction?

    ctors = mbd.getPreferredConstructors();
    if (ctors != null) {
        return autowireConstructor(beanName, mbd, ctors, null);
    }

    // No special handling: simply use no-arg constructor.
    /** 使用默认的构造函数*/
    return instantiateBean(beanName, mbd);
}
```


