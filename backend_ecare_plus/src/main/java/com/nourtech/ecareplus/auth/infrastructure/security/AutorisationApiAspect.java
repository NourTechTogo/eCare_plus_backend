package com.nourtech.ecareplus.auth.infrastructure.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

@Aspect
@Component
@Order(0)
public class AutorisationApiAspect {
    private final AutorisationRessources autorisations;
    public AutorisationApiAspect(AutorisationRessources autorisations) { this.autorisations = autorisations; }

    @Around("execution(public * com.nourtech.ecareplus..api.controller..*(..))"
            + " && !within(com.nourtech.ecareplus.auth.api.controller.AuthentificationController)")
    public Object autoriser(ProceedingJoinPoint call) throws Throwable {
        Method method = ((MethodSignature) call.getSignature()).getMethod();
        autorisations.verifier(method, call.getArgs());
        return autorisations.filtrer(method, call.proceed());
    }
}
