package ru.kotlin.springeventexample.service

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.kotlin.springeventexample.event.ActionType
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2
import java.time.Duration

@Service
class ReactiveSMSService(
    private val businessService: ReactiveBusinessService
) {
    private val logger = LoggerFactory.getLogger(ReactiveSMSService::class.java)

    @PostConstruct
    fun init() {
        // Подписываемся на поток событий BusinessEvent
        businessService.getBusinessEventStream()
            .doOnSubscribe { logger.info("SMS-сервис подписался на поток BusinessEvent") }
//            .onBackpressureBuffer(200) // Добавляем буфер на 200 событий
            .flatMap { event ->
                processBusinessEvent(event)
                    .onErrorResume { error ->
                        logger.error("Ошибка обработки BusinessEvent: ${error.message}", error)
                        Mono.empty()
                    }
            }
            .subscribe(
                { result -> logger.debug("SMS обработано: $result") },
                { error -> logger.error("Ошибка службы СМС: ${error.message}", error) },
                { logger.info("Поток службы SMS завершен") }
            )

        // Подписываемся на поток событий BusinessEvent2
        businessService.getBusinessEvent2Stream()
            .doOnSubscribe { logger.info("SMS-сервис подписался на поток BusinessEvent2") }
            .onBackpressureBuffer(100) // Меньший буфер для BusinessEvent2
            .flatMap { event ->
                processBusinessEvent2(event)
                    .onErrorResume { error ->
                        logger.error("Ошибка обработки BusinessEvent2: ${error.message}", error)
                        Mono.empty()
                    }
            }
            .subscribe(
                { result -> logger.debug("SMS2 обработано: $result") },
                { error -> logger.error("Ошибка службы SMS2: ${error.message}", error) },
                { logger.info("Поток службы SMS2 завершен") }
            )
    }

    private fun processBusinessEvent(event: BusinessEvent): Mono<String> = mono {
        logger.info("Обработка СМС по событию: ${event.payload}")

        // Имитация отправки SMS (1 секунда)
        delay(1000)

        // Бизнес-логика: отправляем SMS только для CREATE и DELETE
        when (event.payload.actionType) {
            ActionType.CREATE -> {
                val message = "SMS: Object ${event.payload.objectId} created"
                logger.info(message)
                message
            }
            ActionType.DELETE -> {
                val message = "SMS: Object ${event.payload.objectId} deleted"
                logger.info(message)
                message
            }
            else -> {
                val message = "SMS: Object ${event.payload.objectId} updated (not sending SMS)"
                logger.debug(message)
                message
            }
        }
    }

    private fun processBusinessEvent2(event: BusinessEvent2): Mono<String> = mono {
        logger.info("Обработка SMS2 для события: ${event.payload} at ${event.time}")

        // Имитация более долгой отправки SMS (2 секунды)
        delay(2000)

        val message = "SMS2: Object ${event.payload.objectId} ${event.payload.actionType} at ${event.time}"
        logger.info(message)
        message
    }

    // Метод для ручной подписки с backpressure
    fun getBusinessEventFlux(): Flux<BusinessEvent> {
        return businessService.getBusinessEventStream()
            .onBackpressureBuffer(
                100, // capacity
                { logger.warn("Переполнение буфера СМС") } // обработчик переполнения
            )
            .delayElements(Duration.ofMillis(100)) // Минимальная задержка между событиями
            .doOnNext { event ->
                logger.debug("получен поток СМС: ${event.payload}")
            }
    }
}