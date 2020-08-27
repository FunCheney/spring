package web;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

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
		tomcat.addWebapp("/","");
		try {
			tomcat.start();//Tomcat启动方法
			tomcat.getServer().await();//Tomcat阻塞方法
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
	}
}
