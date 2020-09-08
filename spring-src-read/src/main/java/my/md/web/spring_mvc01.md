### Spring IOC 在 Web  容器中的载入

 &ensp;&ensp; `Spring IoC` 是一个独立的模块，它并不是直接在 `Web` 容器中发挥作用的，如果
 要在 `Web` 容器中使用 `IoC` 容器，需要 `Spring` 为 `IoC` 设计一个启动过程，把 `IoC` 容器
 导入，并在 `Web` 容器中建立起来。