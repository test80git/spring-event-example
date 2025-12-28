package ru.kotlin.springeventexample.service

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import ru.kotlin.springeventexample.event.BusinessEvent

@Service
class AuditService {

    private val logger = LoggerFactory.getLogger(AuditService::class.java)

    @EventListener(BusinessEvent::class)
    @Async("auditTaskExecutor")
    fun onBusinessEvent(event: BusinessEvent) {
        // уведомление audit

        logger.info("Business event: ${event.payload}")
        logger.info("Business event: ${event}")
    }

}
