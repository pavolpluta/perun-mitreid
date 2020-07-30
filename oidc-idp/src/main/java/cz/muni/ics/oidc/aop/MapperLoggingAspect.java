package cz.muni.ics.oidc.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MapperLoggingAspect {

    public static final Logger log = LoggerFactory.getLogger(MapperLoggingAspect.class);

    @Around("execution(* cz.muni.ics.oidc.models.mappers..* (*))")
    public Object logAroundMethodWithParams(ProceedingJoinPoint pjp) throws Throwable {
        return LoggingUtils.logWithParams(log, pjp);
    }

    @Around("execution(* cz.muni.ics.oidc.models.mappers..* ())")
    public Object logAroundMethodWithoutParams(ProceedingJoinPoint pjp) throws Throwable {
        return LoggingUtils.logWithNoParams(log, pjp);
    }

}
