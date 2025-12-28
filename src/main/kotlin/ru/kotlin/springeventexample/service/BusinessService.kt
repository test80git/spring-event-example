package ru.kotlin.springeventexample.service

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import ru.kotlin.springeventexample.event.ActionInfo
import ru.kotlin.springeventexample.event.ActionType
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2
import java.time.LocalDateTime

@Service()
class BusinessService(
    val publisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(BusinessService::class.java)

    fun create0bject(objectId: Int) {
        // какая-то бизнес-логика
        Thread.sleep(300)
        logger.info("Object with id = $objectId created.")

        val event = BusinessEvent(
            sourceObject = this,
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.CREATE,
            )
        )
        publisher.publishEvent(event)

        logger.info("Event sent.")
    }

    fun update0bject(objectId: Int) {
        // какая-то бизнес-логика
        Thread.sleep(500)
        logger.info("Object with id = $objectId update.")

        val event = BusinessEvent(
            sourceObject = this,
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.UPDATE,
            )
        )
        publisher.publishEvent(event)
        logger.info("Event sent.")
    }

    fun delete0bject(objectId: Int) {
        // какая-то бизнес-логика
        Thread.sleep(300)
        logger.info("Object with id = $objectId deleted.")

        val event2 = BusinessEvent(
            sourceObject = this,
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.DELETE,
            )
        )
        publisher.publishEvent(event2)
        logger.info("Event 2 sent.")

        val event22 = BusinessEvent2(
            sourceObject = this,
            time = LocalDateTime.now(),
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.DELETE,
            )
        )
        publisher.publishEvent(event22)
        logger.info("Event 22 sent.")
    }

}
