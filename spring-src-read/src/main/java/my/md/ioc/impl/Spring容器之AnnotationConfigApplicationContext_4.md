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
&ensp;&ensp;这里将要解析上述代码中的`refresh()`这一部分的代码。。。
### refresh();
<div align="center">
    <img src="https://github.com/FunCheney/spring/blob/master/spring-src-read/src/main/java/my/image/ioc/AnnotationConfigApplication_refresh.jpg">
 </div>