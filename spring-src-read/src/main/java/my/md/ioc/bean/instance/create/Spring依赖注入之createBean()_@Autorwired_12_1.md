## 属性注入的处理
&ensp;&ensp; 在Spring中，通过 `@Autowired` 注入的属性，是通过Bean的后置处理器来完成的。
```java
public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
    try {
        // 注入
        metadata.inject(bean, beanName, pvs);
    }
    catch (BeanCreationException ex) {
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
    }
    return pvs;
}
```
&ensp;&ensp;上述代码就是程序处理的入口，对应的代码在 `AutowiredAnnotationBeanPostProcessor`类中。在该类中采用了模板
模式，定义了两个内部类，来处理 属性注入与方法注入。对应的类分别为 `AutowiredFieldElement` 和 `AutowiredMethodElement`。
在这两个类中，都实现了父类 `InjectionMetadata.InjectedElement` 的 `inject()` 方法来完成注入。


#### 默认使用byType 

#### byName 的选择