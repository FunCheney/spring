## AnnotationConfigApplicationContext 源码解析
```
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
    /**
     * 调用默认的构造方法，由于该类有父类，
     * 故而先调用父类的构造方法，在调用自己的构造方法
     * 在自己的构造方法中初始一个读取器和一个扫描器，
     * 第①部分！！！
     */
    this();
    /**
     * 向spring 的容器中注册bean
     * 第②部分！！！
     */
    register(componentClasses);
    /**
     * 初始化spring的环境
     * 第③部分！！！
     */
    refresh();
}
```
&ensp;&ensp;这里将要解析上述代码中的`register(componentClasses)`这一部分的代码。。。
### register(componentClasses);
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotationConfigApplicationContext_register.jpg">
 </div>

&ensp;&ensp;上述改方法的调用过程，最终就是将定义的MyConfig类对应的BeanDefinition放入到beanDefinitionMap中，
至此beanDefinitionMap中对象又增加了一个，变成6个了。

<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/beanDefinitionMap_six_object.jpg">
 </div>