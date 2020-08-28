package web;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.Set;

/**
 * @author: Fchen
 * @date: 2020/8/28 8:13 上午
 * @desc: TODO
 */
@HandlesTypes(MyWebApplicationInitializer.class)
public class MyServletContainerInitializer implements ServletContainerInitializer {
	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
		for (Class<?> clazz : c) {
			try {
				MyWebApplicationInitializer o = (MyWebApplicationInitializer) clazz.newInstance();
				o.onStartup(ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
