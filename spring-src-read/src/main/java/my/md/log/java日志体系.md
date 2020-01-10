## java日志体系

### jul
### log4j
### jcl
&ensp;&ensp;Jcl不直接记录日志，他是通过第三方日志来记录日志，在没有log4j依赖的情况下，使用jul；
有log4j依赖，就使用log4j。
org.apache.commons.logging.LogFactory中 getLog() 方法。
```java
public static Log getLog(Class clazz) throws LogConfigurationException {
	return getFactory().getInstance(clazz);
}
```
jdk中相关的源码的实现在org.apache.commons.logging.impl.LogFactoryImpl中。
```java
public Log getInstance(Class clazz) throws LogConfigurationException {
	return getInstance(clazz.getName());
}
```
getInstance()方法的实现
```java
public Log getInstance(String name) throws LogConfigurationException {
    Log instance = (Log) instances.get(name);
    if (instance == null) {
        instance = newInstance(name);
        instances.put(name, instance);
    }
    return instance;
}
```

### slf4j

### logback
