package cz.muni.ics.oidc.aop;

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
		String methodName = pjp.getSignature().getName();
		String className = pjp.getTarget().getClass().getName();
		Object[] args = pjp.getArgs();


		log.trace("{} - {}({})", className, methodName, args);
		try {
			Object result = pjp.proceed();
			log.trace("{} - {}({}) returns: {}", className, methodName, args, result);
			return result;
		} catch (Throwable e) {
			log.warn("{} - {}({}) has thrown {}", className, methodName, args, e.getClass(), e);
			throw e;
		}
	}

	@Around("execution(* cz.muni.ics.oidc.server..* ())")
	public Object logStartNoParams(ProceedingJoinPoint pjp) throws Throwable {
		String methodName = pjp.getSignature().getName();
		String className = pjp.getTarget().getClass().getName();

		log.trace("{} - {}()", className, methodName);
		try {
			Object result = pjp.proceed();
			log.trace("{} - {}() returns: {}", className, methodName, result);
			return result;
		} catch (Throwable e) {
			log.warn("{} - {}() has thrown {}", className, methodName, e.getClass(), e);
			throw e;
		}
	}
}
