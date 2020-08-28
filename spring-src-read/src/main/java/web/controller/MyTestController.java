package web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Fchen
 * @date: 2020/8/27 1:45 下午
 * @desc: TODO
 */
@RestController
public class MyTestController {
	@RequestMapping("/hello.do")
	public String hello() {
		return "hello,this is Fchen test";
	}
}
