## Spring依赖注入之createBean()
### 自动装配的实现
&ensp;&ensp;在前面IoC容器的实现原理分析中，一直是通过BeanDefinition的属性和构造函数以显式的方式对Bean
的依赖进行管理的。在Spring中，相对这种现实的依赖管理方式，IoC容器还提供了自动装配的方式，为应用使用容器提供
更大的方便。在自动装配中不需要对Bean属性做显式的依赖关系声明，只需要配置好autowiring属性，IoC容器会根据这
个属性的配置，使用反射自动查找属性的类型或者名字，然后基于属性的类型或名字来自动匹配IoC容器中的Bean，从而自  
动地完成依赖注入。

&ensp;&ensp;从autowiring使用上可以知道，这个 autowiring 属性在对 Bean
属性进行依赖注入是起作用。对 autowiring 属性进行处理，从而完成对 Bean
属性的租定依赖装配，是在 populateBean 中实现的。在 populateBean
的实现中，在处理一般的 Bean 之前，先对 autowiring 属性进行处理。如果当前的 Bean
配置了 autowire_by_name 或者 autowire_by_type 属性，那么调用相应的
autowireByName 方法和 autowireByType方法。这两个方法巧妙的应用了 IoC容器的特性。

#### 1. autowireByName

```java
protected void autowireByName(
        String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

    //寻找bw中需要依赖注入的属性
    String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
    for (String propertyName : propertyNames) {
        if (containsBean(propertyName)) {
            // 使用取得当前bean的属性名作为bean的名字，向IOC容器索取bean
            Object bean = getBean(propertyName);
            // 把从容器中得到的bean设置到当前Bean的属性中去
            pvs.add(propertyName, bean);
            registerDependentBean(propertyName, beanName);
            if (logger.isTraceEnabled()) {
                logger.trace("Added autowiring by name from bean name '" + beanName +
                        "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
            }
        }
        else {
            if (logger.isTraceEnabled()) {
                logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                        "' by name: no matching bean found");
            }
        }
    }
}
```
&ensp;&ensp;autowire_by_name，它首先通过反射机制从当前Bean中得到需要注入的属性名，然后使用这个属性名向
容器申请与之同名的Bean，这样又触发了另一个Bean的生成和依赖注入的过程。

&ensp;&ensp;对autowireByName来说，它首先需要得到当前Bean的属性名，这些属性名已经在
BeanWrapper 和 BeanDefinition
中封装好了，然后是对这一系列属性名进行匹配的过程。在匹配的过程中，因为已经有了属性的名字，所以可以直接使用
属性名作为Bean名字向容器索取Bean，这个getBean会触发当前Bean的依赖Bean的依赖注入，从而得到属性对应的依赖
Bean。在执行完这个getBean后，把这个依赖Bean注入到当前Bean的属性中去，这样就完成了通过这个依赖属性名完成自  
动依赖注入的过程。

#### 2. autowireByType
```java
protected void autowireByType(
        String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

    TypeConverter converter = getCustomTypeConverter();
    if (converter == null) {
        converter = bw;
    }

    Set<String> autowiredBeanNames = new LinkedHashSet<>(4);

    //寻找bw中需要依赖注入的属性
    String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
    for (String propertyName : propertyNames) {
        try {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            // Don't try autowiring by type for type Object: never makes sense,
            // even if it technically is a unsatisfied, non-simple property.
            if (Object.class != pd.getPropertyType()) {
                // 探测指定属性的set方法
                MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                // Do not allow eager init for type matching in case of a prioritized post-processor.
                boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
                //解析指定beanName的属性所匹配的值，并把解析到的属性名称存储在autowireBeanNames中
                DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                if (autowiredArgument != null) {
                    pvs.add(propertyName, autowiredArgument);
                }
                for (String autowiredBeanName : autowiredBeanNames) {
                    //注册依赖
                    registerDependentBean(autowiredBeanName, beanName);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
                                propertyName + "' to bean named '" + autowiredBeanName + "'");
                    }
                }
                autowiredBeanNames.clear();
            }
        }
        catch (BeansException ex) {
            throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
        }
    }
}
```
