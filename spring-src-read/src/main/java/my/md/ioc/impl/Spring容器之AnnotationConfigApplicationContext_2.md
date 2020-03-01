
1.this.reader = new AnnotatedBeanDefinitionReader(this);
    
    beanDefinitionMap中添加了5个BeanDefinition
      a.org.springframework.context.annotation.internalConfigurationAnnotationProcessor
      b.org.springframework.context.event.internalEventListenerFactory
      c.org.springframework.context.event.internalEventListenerProcessor
      d.org.springframework.context.annotation.internalAutowiredAnnotationProcessor
      f.org.springframework.context.annotation.internalCommonAnnotationProcessor
      
      
      
| 常量  | 对应的BeanPostProcessor	| 对应的注解	| 
|---|---|---|
|CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME| ConfigurationClassPostProcessor | @Configuration|
|AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME| AutowiredAnnotationBeanPostProcessor | @AutoWired |
|REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME | RequiredAnnotationBeanPostProcessor	| @Required |
|COMMON_ANNOTATION_PROCESSOR_BEAN_NAME| CommonAnnotationBeanPostProcessor | @PostConstruct  @PreDestroy |
|EVENT_LISTENER_PROCESSOR_BEAN_NAME| EventListenerMethodProcessor | @EventListener |
|EVENT_LISTENER_FACTORY_BEAN_NAME| EventListenerFactory | EventListener |
