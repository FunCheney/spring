package com.fchen.config;

import com.fchen.expand.MyImportSelector;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author: Fchen
 * @date: 2020/5/14 10:16 下午
 * @desc: TODO
 */
@Configuration
@Import(MyImportSelector.class)
public class MySelectorImportConfig {
}
