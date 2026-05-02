package com.n11bootcamp.order_service.aop;

import com.n11bootcamp.order_service.service.TelegramService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    private final TelegramService telegramService;

    public LoggingAspect(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Pointcut("within(com.n11bootcamp.order_service.service.impl..*) || within(com.n11bootcamp.order_service.controller..*) || within(com.n11bootcamp.order_service.saga..*)")
    public void applicationPackagePointcut() {
    }

    @Before("applicationPackagePointcut()")
    public void logBefore(JoinPoint joinPoint) {
        LOGGER.info("Entering method: {} with arguments: {}", 
            joinPoint.getSignature().toShortString(), 
            Arrays.toString(joinPoint.getArgs()));
    }

    @AfterThrowing(pointcut = "applicationPackagePointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        LOGGER.error("Exception in method: {} with message: {} and cause: {}", 
            joinPoint.getSignature().toShortString(), 
            e.getMessage(), 
            e.getCause() != null ? e.getCause() : "NULL");

        // Send Telegram Notification
        String telegramMsg = String.format("🚨 *ERROR IN ORDER-SERVICE* 🚨\n\n📍 *Method:* %s\n❌ *Message:* %s", 
            joinPoint.getSignature().toShortString(), e.getMessage());
        telegramService.sendMessage(telegramMsg);
    }
}
