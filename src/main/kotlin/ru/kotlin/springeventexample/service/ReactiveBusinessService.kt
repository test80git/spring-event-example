package ru.kotlin.springeventexample.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import ru.kotlin.springeventexample.event.ActionInfo
import ru.kotlin.springeventexample.event.ActionType
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2
import java.time.LocalDateTime

@Service
class ReactiveBusinessService(
    private val businessEventSink: Sinks.Many<BusinessEvent>,
    private val businessEvent2Sink: Sinks.Many<BusinessEvent2>
) {
    private val logger = LoggerFactory.getLogger(ReactiveBusinessService::class.java)

    // Реактивная версия создания объекта
    suspend fun createObject(objectId: Int): Mono<String> = mono {
        logger.info("Starting creation of object with id = $objectId")

        // Имитация работы с БД (корутины вместо Thread.sleep)
        delay(300)

        logger.info("Object with id = $objectId created.")

        val event = BusinessEvent(
            sourceObject = this,
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.CREATE,
            )
        )

        // Публикуем событие реактивно
        businessEventSink.tryEmitNext(event)

        logger.info("Event sent for object $objectId")
        "Object with id = $objectId created"
    }

    suspend fun updateObject(objectId: Int): Mono<String> = mono {
        logger.info("Starting update of object with id = $objectId")

        delay(500)

        logger.info("Object with id = $objectId updated.")

        val event = BusinessEvent(
            sourceObject = this,
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.UPDATE,
            )
        )

        businessEventSink.tryEmitNext(event)

        logger.info("Event sent for object $objectId")
        "Object with id = $objectId updated"
    }

    suspend fun deleteObject(objectId: Int): Mono<String> = mono {
        logger.info("Starting deletion of object with id = $objectId")

        delay(300)

        logger.info("Object with id = $objectId deleted.")

        // Первое событие
        val event1 = BusinessEvent(
            sourceObject = this,
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.DELETE,
            )
        )

        // Второе событие
        val event2 = BusinessEvent2(
            sourceObject = this,
            time = LocalDateTime.now(),
            payload = ActionInfo(
                objectId = objectId,
                actionType = ActionType.DELETE,
            )
        )

        // Публикуем оба события
        businessEventSink.tryEmitNext(event1)
        businessEvent2Sink.tryEmitNext(event2)

        logger.info("Both events sent for object $objectId")
        "Object with id = $objectId deleted"
    }

    // Получаем поток событий для подписки
    fun getBusinessEventStream() = businessEventSink.asFlux()
    fun getBusinessEvent2Stream() = businessEvent2Sink.asFlux()
}