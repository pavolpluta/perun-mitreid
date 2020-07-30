package cz.muni.ics.oidc.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;

import java.sql.Timestamp;

/**
 * Utility class that takes care of the logging for AOP.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class LoggingUtils {

    /**
     * Log at TRACE level start and end of method without parameters
     * @param log Logger object
     * @param pjp proceeding join point
     * @return Value returned by the methods
     * @throws Throwable throw exception by the method execution
     */
    public static Object logWithNoParams(Logger log, ProceedingJoinPoint pjp) throws Throwable {
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

    /**
     * Log at TRACE level start and end of method with parameters
     * @param log Logger object
     * @param pjp proceeding join point
     * @return Value returned by the methods
     * @throws Throwable throw exception by the method execution
     */
    public static Object logWithParams(Logger log, ProceedingJoinPoint pjp) throws Throwable {
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

    /**
     * Log at TRACE level times of start and end of method execution without parameters
     * @param log Logger object
     * @param pjp proceeding join point
     * @return Value returned by the methods
     * @throws Throwable throw exception by the method execution
     */
    public static Object logExecutionWithParams(Logger log, ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getName();
        Object[] args = pjp.getArgs();
        long start = System.currentTimeMillis();

        log.trace("Execution of {}.{}({}) started at {}", className, methodName, args, new Timestamp(start));
        try {
            Object result = pjp.proceed();
            long finish = System.currentTimeMillis();
            log.trace("Execution of {}.{}({}) finished successfully at {}, execution took {}ms",
                    className, methodName, args, new Timestamp(finish), finish - start);
            return result;
        } catch (Throwable e) {
            long finish = System.currentTimeMillis();
            log.trace("Execution of {}.{}({}) finished by exception being thrown at {}, execution took {}ms",
                    className, methodName, args, new Timestamp(finish), finish - start);
            throw e;
        }
    }

    /**
     * Log at TRACE level times of start and end of method execution without parameters
     * @param log Logger object
     * @param pjp proceeding join point
     * @return Value returned by the methods
     * @throws Throwable throw exception by the method execution
     */
    public static Object logExecutionWithNoParams(Logger log, ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getName();
        long start = System.currentTimeMillis();

        log.trace("Execution of {}.{}() started at {}", className, methodName, new Timestamp(start));
        try {
            Object result = pjp.proceed();
            long finish = System.currentTimeMillis();
            log.trace("Execution of {}.{}() finished successfully at {}, execution took {}ms",
                    className, methodName, new Timestamp(finish), finish - start);
            return result;
        } catch (Throwable e) {
            long finish = System.currentTimeMillis();
            log.trace("Execution of {}.{}() finished by exception being thrown at {}, execution took {}ms",
                    className, methodName, new Timestamp(finish), finish - start);
            throw e;
        }
    }

}
