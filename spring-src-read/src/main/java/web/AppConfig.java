package web;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author: Fchen
 * @date: 2020/8/11 9:19 上午
 * @desc: TODO
 */
@Configuration
@ComponentScan("web")
public class AppConfig implements WebMvcConfigurer {
	@Override
	public void configureViewResolvers(ViewResolverRegistry registry){
		registry.jsp("/",".jsp");
	}
}
