package cz.muni.ics.oidc;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;

public class LoggingUtils {

    public static Object logWithNoParams(Logger log, ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getName();

        log.trace("{} --- {}()", className, methodName);
        try {
            Object result = pjp.proceed();
            log.trace("{} --- {}() returns: {}", className, methodName, result);
            return result;
        } catch (Throwable e) {
            log.warn("{} --- {}() has thrown {}", className, methodName, e.getClass(), e);
            throw e;
        }
    }

    public static Object logWithParams(Logger log, ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getName();
        Object[] args = pjp.getArgs();

        log.trace("{} --- {}({})", className, methodName, args);
        try {
            Object result = pjp.proceed();
            log.trace("{} --- {}({}) returns: {}", className, methodName, args, result);
            return result;
        } catch (Throwable e) {
            log.warn("{} --- {}({}) has thrown {}", className, methodName, args, e.getClass(), e);
            throw e;
        }
    }
}
