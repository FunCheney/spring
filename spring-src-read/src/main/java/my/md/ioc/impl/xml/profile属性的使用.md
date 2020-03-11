### profile 用法
#### 示例
```
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:jdbc="http://www.springframework.org/schema/jdbc"
    xmlns:jee="http://www.springframework.org/schema/jee"
    xsi:schemaLocation="...">

    <!-- other bean definitions -->

    <beans profile="development">
        <jdbc:embedded-database id="dataSource">
            <jdbc:script location="classpath:com/bank/config/sql/schema.sql"/>
            <jdbc:script location="classpath:com/bank/config/sql/test-data.sql"/>
        </jdbc:embedded-database>
    </beans>

    <beans profile="production">
        <jee:jndi-lookup id="dataSource" jndi-name="java:comp/env/jdbc/datasource"/>
    </beans>

```
&ensp;&ensp;继承到web环境中时，在web.xml中加入以下代码

```
<context-param>
        <param-name>spring.profiles.active</param-name>
        <param-value>dev</param-value>
    </context-param>
```

&ensp;&ensp;有了这个特性，就可以同时在配置文件中部署两套配置来适用于生产环境和开发环境，这样就可以方便的进行
切换开发、部署环境，最长适用的就是跟换不同环境的数据库。

&ensp;&ensp;了解了 profile 的使用来分析代码会清晰很多，首先程序会获取 bean 结点是否定义了 profile 属性，
如果定义了则会需要到环境变量中去寻找。
