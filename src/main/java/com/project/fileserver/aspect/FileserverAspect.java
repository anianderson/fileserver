package com.project.fileserver.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.project.fileserver.model.RequiredObject;
import com.project.fileserver.utils.CommonServiceUtils;

import lombok.extern.log4j.Log4j2;

@Component
@Aspect
@Log4j2
public class FileserverAspect {

	@Autowired
	private CommonServiceUtils commonService;

	@Before(value = "execution(* com.project.fileserver.service.FileserverService.*(com.project.fileserver.model.RequiredObject,..)) and args(requiredObject,..)", argNames = "requiredObject")
	public void refactorBucketName(JoinPoint joinPoint, RequiredObject requiredObject) {
		requiredObject.setBucket(commonService.refactorBucketName(requiredObject.getBucket()));
	}

	@Around("execution(* com.project.fileserver..*(..)) && !within(com.project.fileserver.aspect.*)")
	public Object logAppender(ProceedingJoinPoint joinPoint) throws Throwable {
		String methodname = joinPoint.getSignature().toShortString();
		long start = System.currentTimeMillis();
		log.debug("{} starting...", methodname);
		Object obj = joinPoint.proceed();
		long end = System.currentTimeMillis();
		log.debug("{} finished in {} ms", methodname, end - start);
		return obj;
	}

}
