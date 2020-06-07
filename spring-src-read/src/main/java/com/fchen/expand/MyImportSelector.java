//package com.fchen.expand;
//
//import com.fchen.service.MySelectImportService;
//import org.springframework.context.annotation.ImportSelector;
//import org.springframework.core.type.AnnotationMetadata;
//
///**
// * @author: Fchen
// * @date: 2020/5/13 11:03 下午
// * @desc: MyImportSelector 的应用
// */
//public class MyImportSelector implements ImportSelector {
//	@Override
//	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
//		return new String[]{MySelectImportService.class.getName()};
//	}
//}
