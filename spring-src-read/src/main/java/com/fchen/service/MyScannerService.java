package com.fchen.service;

import com.fchen.dao.mapper.MyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: Fchen
 * @date: 2020/5/12 11:27 下午
 * @desc: TODO
 */
@Service
public class MyScannerService {
	@Autowired
	MyMapper myMapper;

	public void testMyScan(){
		myMapper.myScan();
	}
}
