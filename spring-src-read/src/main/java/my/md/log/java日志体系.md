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
&ensp;&ensp;jdk中相关的源码的实现在org.apache.commons.logging.impl.LogFactoryImpl中。
```java
public Log getInstance(Class clazz) throws LogConfigurationException {
	return getInstance(clazz.getName());
}
```
&ensp;&ensp;getInstance()方法的实现
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
&ensp;&ensp;newInstance()方法实现
```java
protected Log newInstance(String name) throws LogConfigurationException {
    Log instance;
    try {
        if (logConstructor == null) {
            instance = discoverLogImplementation(name);
        }
        else {
            Object params[] = { name };
            instance = (Log) logConstructor.newInstance(params);
        }

        if (logMethod != null) {
            Object params[] = { this };
            logMethod.invoke(instance, params);
        }

        return instance;

    } catch (LogConfigurationException lce) {

        // this type of exception means there was a problem in discovery
        // and we've already output diagnostics about the issue, etc.;
        // just pass it on
        throw lce;

    } catch (InvocationTargetException e) {
        // A problem occurred invoking the Constructor or Method
        // previously discovered
        Throwable c = e.getTargetException();
        throw new LogConfigurationException(c == null ? e : c);
    } catch (Throwable t) {
        handleThrowable(t); // may re-throw t
        // A problem occurred invoking the Constructor or Method
        // previously discovered
        throw new LogConfigurationException(t);
    }
}
```
&ensp;&ensp;discoverLogImplementation()方法的实现
```java
private Log discoverLogImplementation(String logCategory)
    throws LogConfigurationException {
    if (isDiagnosticsEnabled()) {
        logDiagnostic("Discovering a Log implementation...");
    }

    initConfiguration();

    Log result = null;

    // See if the user specified the Log implementation to use
    String specifiedLogClassName = findUserSpecifiedLogClassName();

    if (specifiedLogClassName != null) {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Attempting to load user-specified log class '" +
                specifiedLogClassName + "'...");
        }

        result = createLogFromClass(specifiedLogClassName,
                                    logCategory,
                                    true);
        if (result == null) {
            StringBuffer messageBuffer =  new StringBuffer("User-specified log class '");
            messageBuffer.append(specifiedLogClassName);
            messageBuffer.append("' cannot be found or is not useable.");

            // Mistyping or misspelling names is a common fault.
            // Construct a good error message, if we can
            informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_LOG4J_LOGGER);
            informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_JDK14_LOGGER);
            informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_LUMBERJACK_LOGGER);
            informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_SIMPLE_LOGGER);
            throw new LogConfigurationException(messageBuffer.toString());
        }

        return result;
    }

    if (isDiagnosticsEnabled()) {
        logDiagnostic(
            "No user-specified Log implementation; performing discovery" +
            " using the standard supported logging implementations...");
    }
    for(int i=0; i<classesToDiscover.length && result == null; ++i) {
        result = createLogFromClass(classesToDiscover[i], logCategory, true);
    }

    if (result == null) {
        throw new LogConfigurationException
                    ("No suitable Log implementation");
    }

    return result;
}
```
&ensp;&esnp;上述代码中的**classesToDiscover**数组中的常量
```java
private static final String[] classesToDiscover = {
            LOGGING_IMPL_LOG4J_LOGGER,
            "org.apache.commons.logging.impl.Jdk14Logger",
            "org.apache.commons.logging.impl.Jdk13LumberjackLogger",
            "org.apache.commons.logging.impl.SimpleLog"
};
```
&ensp;&ensp;createLogFromClass()方法的实现
```java
private Log createLogFromClass(String logAdapterClassName,
                               String logCategory,
                               boolean affectState)
    throws LogConfigurationException {

    if (isDiagnosticsEnabled()) {
        logDiagnostic("Attempting to instantiate '" + logAdapterClassName + "'");
    }

    Object[] params = { logCategory };
    Log logAdapter = null;
    Constructor constructor = null;

    Class logAdapterClass = null;
    ClassLoader currentCL = getBaseClassLoader();

    for(;;) {
        // Loop through the classloader hierarchy trying to find
        // a viable classloader.
        logDiagnostic("Trying to load '" + logAdapterClassName + "' from classloader " + objectId(currentCL));
        try {
            if (isDiagnosticsEnabled()) {
                // Show the location of the first occurrence of the .class file
                // in the classpath. This is the location that ClassLoader.loadClass
                // will load the class from -- unless the classloader is doing
                // something weird.
                URL url;
                String resourceName = logAdapterClassName.replace('.', '/') + ".class";
                if (currentCL != null) {
                    url = currentCL.getResource(resourceName );
                } else {
                    url = ClassLoader.getSystemResource(resourceName + ".class");
                }

                if (url == null) {
                    logDiagnostic("Class '" + logAdapterClassName + "' [" + resourceName + "] cannot be found.");
                } else {
                    logDiagnostic("Class '" + logAdapterClassName + "' was found at '" + url + "'");
                }
            }

            Class c;
            try {
                c = Class.forName(logAdapterClassName, true, currentCL);
            } catch (ClassNotFoundException originalClassNotFoundException) {
                // The current classloader was unable to find the log adapter
                // in this or any ancestor classloader. There's no point in
                // trying higher up in the hierarchy in this case..
                String msg = originalClassNotFoundException.getMessage();
                logDiagnostic("The log adapter '" + logAdapterClassName + "' is not available via classloader " +
                              objectId(currentCL) + ": " + msg.trim());
                try {
                    // Try the class classloader.
                    // This may work in cases where the TCCL
                    // does not contain the code executed or JCL.
                    // This behaviour indicates that the application
                    // classloading strategy is not consistent with the
                    // Java 1.2 classloading guidelines but JCL can
                    // and so should handle this case.
                    c = Class.forName(logAdapterClassName);
                } catch (ClassNotFoundException secondaryClassNotFoundException) {
                    // no point continuing: this adapter isn't available
                    msg = secondaryClassNotFoundException.getMessage();
                    logDiagnostic("The log adapter '" + logAdapterClassName +
                                  "' is not available via the LogFactoryImpl class classloader: " + msg.trim());
                    break;
                }
            }

            constructor = c.getConstructor(logConstructorSignature);
            Object o = constructor.newInstance(params);

            // Note that we do this test after trying to create an instance
            // [rather than testing Log.class.isAssignableFrom(c)] so that
            // we don't complain about Log hierarchy problems when the
            // adapter couldn't be instantiated anyway.
            if (o instanceof Log) {
                logAdapterClass = c;
                logAdapter = (Log) o;
                break;
            }

            // Oops, we have a potential problem here. An adapter class
            // has been found and its underlying lib is present too, but
            // there are multiple Log interface classes available making it
            // impossible to cast to the type the caller wanted. We
            // certainly can't use this logger, but we need to know whether
            // to keep on discovering or terminate now.
            //
            // The handleFlawedHierarchy method will throw
            // LogConfigurationException if it regards this problem as
            // fatal, and just return if not.
            handleFlawedHierarchy(currentCL, c);
        } catch (NoClassDefFoundError e) {
            // We were able to load the adapter but it had references to
            // other classes that could not be found. This simply means that
            // the underlying logger library is not present in this or any
            // ancestor classloader. There's no point in trying higher up
            // in the hierarchy in this case..
            String msg = e.getMessage();
            logDiagnostic("The log adapter '" + logAdapterClassName +
                          "' is missing dependencies when loaded via classloader " + objectId(currentCL) +
                          ": " + msg.trim());
            break;
        } catch (ExceptionInInitializerError e) {
            // A static initializer block or the initializer code associated
            // with a static variable on the log adapter class has thrown
            // an exception.
            //
            // We treat this as meaning the adapter's underlying logging
            // library could not be found.
            String msg = e.getMessage();
            logDiagnostic("The log adapter '" + logAdapterClassName +
                          "' is unable to initialize itself when loaded via classloader " + objectId(currentCL) +
                          ": " + msg.trim());
            break;
        } catch (LogConfigurationException e) {
            // call to handleFlawedHierarchy above must have thrown
            // a LogConfigurationException, so just throw it on
            throw e;
        } catch (Throwable t) {
            handleThrowable(t); // may re-throw t
            // handleFlawedDiscovery will determine whether this is a fatal
            // problem or not. If it is fatal, then a LogConfigurationException
            // will be thrown.
            handleFlawedDiscovery(logAdapterClassName, currentCL, t);
        }

        if (currentCL == null) {
            break;
        }

        // try the parent classloader
        // currentCL = currentCL.getParent();
        currentCL = getParentClassLoader(currentCL);
    }

    if (logAdapterClass != null && affectState) {
        // We've succeeded, so set instance fields
        this.logClassName   = logAdapterClassName;
        this.logConstructor = constructor;

        // Identify the <code>setLogFactory</code> method (if there is one)
        try {
            this.logMethod = logAdapterClass.getMethod("setLogFactory", logMethodSignature);
            logDiagnostic("Found method setLogFactory(LogFactory) in '" + logAdapterClassName + "'");
        } catch (Throwable t) {
            handleThrowable(t); // may re-throw t
            this.logMethod = null;
            logDiagnostic("[INFO] '" + logAdapterClassName + "' from classloader " + objectId(currentCL) +
                          " does not declare optional method " + "setLogFactory(LogFactory)");
        }

        logDiagnostic("Log adapter '" + logAdapterClassName + "' from classloader " +
                      objectId(logAdapterClass.getClassLoader()) + " has been selected for use.");
    }

    return logAdapter;
}
```
&ensp;&ensp;最后调用的forName()方法
```java
public static Class<?> forName(String name, boolean initialize,
                               ClassLoader loader)
    throws ClassNotFoundException {
    Class<?> caller = null;
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
        // Reflective call to get caller class is only needed if a security manager
        // is present.  Avoid the overhead of making this call otherwise.
        caller = Reflection.getCallerClass();
        if (sun.misc.VM.isSystemDomainLoader(loader)) {
            ClassLoader ccl = ClassLoader.getClassLoader(caller);
            if (!sun.misc.VM.isSystemDomainLoader(ccl)) {
                sm.checkPermission(
                    SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
        }
    }
    return forName0(name, initialize, loader, caller);
}
```

### slf4j

### logback
