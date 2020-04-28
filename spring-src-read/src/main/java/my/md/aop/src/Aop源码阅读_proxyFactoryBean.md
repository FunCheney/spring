## ProxyFactoryBean
&ensp;&ensp;通过 ProxyFactoryBean 来配置目标对象和切面行为。ProxyFactoryBean 是一个 FactoryBean。
在 ProxyFactoryBean 中，通过 interceptorNames 属性来配置已经定义好的通知器 Advisors。虽然名字是
interceptorNames，但实际上却提供了 Aop 应用配置通知器的地方。在 ProxyFactoryBean 中需要为 target  
目标生成 Proxy 代理对象，从而为 Aop 横切面的编织做好准备工作。

