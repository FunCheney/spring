package web;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * @author: Fchen
 * @date: 2020/8/27 1:42 下午
 * @desc: TODO
 */
public class MyApplication {
	public static void run() {
		//实例化tomcat对象
		Tomcat tomcat =new Tomcat();
		//端口号设置
		tomcat.setPort(9090);
		String path = MyApplication.class.getResource("/").getPath();
		// 告诉tomcat 源码在哪里
		Context context = tomcat.addWebapp("/", "/");
		WebResourceRoot resourceRoot = new StandardRoot(context);
		resourceRoot.addPreResources(new DirResourceSet(resourceRoot,"/WEB-INF/class",path, "/"));
		try {
			//Tomcat启动方法
			tomcat.start();
			//Tomcat阻塞方法
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
	}
}
