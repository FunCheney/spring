### 构造器注入与循环依赖 

&ensp;&ensp;依赖注入入口：
```java
argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
								getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);

```