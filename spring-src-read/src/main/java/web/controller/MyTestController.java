package web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author: Fchen
 * @date: 2020/8/27 1:45 下午
 * @desc: TODO
 */
@Controller
public class MyTestController {
	@RequestMapping("hello")
	public String index() {
		return "hello,this is Fchen test";
	}
}
