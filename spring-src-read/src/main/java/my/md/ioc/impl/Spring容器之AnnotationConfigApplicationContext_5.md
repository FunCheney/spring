## AnnotationConfigApplicationContext 源码解析
&ensp;&ensp;之前的文章介绍到了，`AbstractApplicationContext.refresh()`方法，在改方法中
有十几个流程，这篇文章将重点介绍啊`invokeBeanFactoryPostProcessors(beanFactory)`。这个
代码是重点流程，这篇文章详细分析一下。
### invokeBeanFactoryPostProcessors()
#### 该方法调用流过程
第①步：AbstractApplicationContext#invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory)

第②步：PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

        2.1 registryProcessor.postProcessBeanDefinitionRegistry(registry) 处理自定义的 BeanDefinitionRegistryPostProcessor子类
        2.2 DefaultListableBeanFactory.getBeanNamesForType(java.lang.Class<?>, boolean, boolean) 通过type得到 得到一个Ben的名称

第③步：PostProcessorRegistrationDelegate#invokeBeanDefinitionRegistryPostProcessors() spring内部自己实现了BeanDefinitionRegistryPostProcessor接口

第④步：postProcessor.postProcessBeanDefinitionRegistry(registry) 不同的子类去自己的实现类中处理，在这里，spring内部的目前为止只有一个实现，
那就是ConfigurationClassPostProcessor类，改了是spring内置的。

第⑤步：ConfigurationClassPostProcessor#processConfigBeanDefinitions(),处理 ConfigurationClassPostProcessor



