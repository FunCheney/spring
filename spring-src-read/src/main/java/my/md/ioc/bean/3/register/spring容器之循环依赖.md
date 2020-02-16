### spring 之循环依赖

#### 循环依赖
&ensp;&ensp;循环依赖就是循环引用，就是两个或多个Bean之间相互持有对方。循环引用是无法解决的，
除非有终结条件，否则就是死循环，最终导致内存溢出。

#### spring 如何解决循环依赖

##### 1.构造器循环依赖
&ensp;&ensp;通过构造器注入的循环依赖，这种方式的循环依赖是无法解决的，只能抛出BeanCurrentlyInCreationException异常。
通过该异常来表示循环依赖。

&ensp;&ensp;Spring容器将每一个正在创建的Bean标识符放在一个"当前创建的Bean池(Map)中"，Bean标识符在创建过程中将一直保持在其中，
因此在创建Bean的过程中发现自己已经在"当前创建的Bean池(Map)中"，将抛出BeanCurrentlyInCreationException表示循环依赖；对于创建
完毕的Bean，就会在"当前创建Bean池"中清除掉。

##### 2.setter方法循环依赖
&ensp;&ensp;表示通过setter注入方式构成循环依赖。对于setter注入造成的依赖是通过spring容器提前暴露刚完成构造器的注入
但未完成其他步骤(如 setter)的Bean来完成的，而且只能解决单例作用域的Bean循环依赖。通过提前暴露一个单例工厂方法，从而使
其他Bean能引用到该Bean。

##### 3.prototype范围的循环依赖
&ensp;&ensp;对于"prototype"作用域bean，Spring容器无法完成依赖注入，因为Spring容器不进行缓存prototype作用域的
Bean，因此无法提前暴露一个创建中的Bean。