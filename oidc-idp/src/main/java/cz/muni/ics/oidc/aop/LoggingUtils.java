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
     * Log at TRACE level start and end of method.
     * @param log Logger object.
     * @param pjp proceeding join point.
     * @return Value returned by the methods.
     * @throws Throwable throw exception by the method execution.
     */
    public static Object logExecutionStartAndEnd(Logger log, ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getName();
        Object[] args = pjp.getArgs();

        log.trace("{} - {}({})", className, methodName, args);
        try {
            Object result = pjp.proceed();
            log.trace("{} - {}({}) returns: {}", className, methodName, args.length > 0 ? args : "", result);
            return result;
        } catch (Throwable e) {
            log.warn("{} - {}({}) has thrown {}", className, methodName, args.length > 0 ? args : "", e.getClass(), e);
            throw e;
        }
    }

    /**
     * Log at TRACE level times of start and end of method execution.
     * @param log Logger object.
     * @param pjp proceeding join point.
     * @return Value returned by the methods.
     * @throws Throwable throw exception by the method execution.
     */
    public static Object logExectuionTimes(Logger log, ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getSimpleName();
        Object[] args = pjp.getArgs();
        long start = System.currentTimeMillis();

        log.trace("Execution of {}.{}({}) started at {}", className, methodName, args.length > 0 ? args : "", new Timestamp(start));
        try {
            Object result = pjp.proceed();
            long finish = System.currentTimeMillis();
            log.trace("Execution of {}.{}({}) finished successfully at {}, execution took {}ms",
                    className, methodName, args.length > 0 ? args : "", new Timestamp(finish), finish - start);
            return result;
        } catch (Throwable e) {
            long finish = System.currentTimeMillis();
            log.trace("Execution of {}.{}({}) finished by exception being thrown at {}, execution took {}ms",
                    className, methodName, args.length > 0 ? args : "", new Timestamp(finish), finish - start);
            throw e;
        }
    }

}
