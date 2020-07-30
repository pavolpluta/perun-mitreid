package cz.muni.ics.oidc.aop;

import cz.muni.ics.oidc.LoggingUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServerLoggingAspect {

	public static final Logger log = LoggerFactory.getLogger(ServerLoggingAspect.class);

	@Around("execution(* cz.muni.ics.oidc.server..* (*))")
	public Object logStart(ProceedingJoinPoint pjp) throws Throwable {
		return LoggingUtils.logWithParams(log, pjp);
	}

	@Around("execution(* cz.muni.ics.oidc.server..* ())")
	public Object logStartNoParams(ProceedingJoinPoint pjp) throws Throwable {
		return LoggingUtils.logWithNoParams(log, pjp);
	}
}
