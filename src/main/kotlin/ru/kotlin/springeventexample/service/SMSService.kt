package ru.kotlin.springeventexample.service

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2

@Service
class SMSService {

    private val logger = LoggerFactory.getLogger(SMSService::class.java)

    @EventListener(BusinessEvent::class)
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // если работаете с БД
    @Async("smsTaskExecutor")
    fun onBusinessEvent(event: BusinessEvent) {
        // уведомление по смс
        Thread.sleep(1000)

        logger.info("SMS Business event: ${event}")
    }

    @EventListener(BusinessEvent2::class)
    @Async("smsTaskExecutor")
    fun onBusinessEvent2(event: BusinessEvent2) {
        // уведомление по смс2
        Thread.sleep(2000)

        logger.info("SMS2 Business event: $event")
    }

}
