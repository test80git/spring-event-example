package ru.kotlin.springeventexample.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactiveAuditService(
    private val businessService: ReactiveBusinessService
) {
    private val logger = LoggerFactory.getLogger(ReactiveAuditService::class.java)

    @PostConstruct
    fun init() {
        // Быстрая обработка всех событий для аудита
        businessService.getBusinessEventStream()
            .doOnSubscribe { logger.info("Служба аудита подписалась на поток BusinessEvent") }
            .flatMap { event ->
                Mono.fromCallable {
                    // Быстрая запись в лог без задержек
                    logger.info("AUDIT LOG: ${event.payload}")
                    event.payload.objectId
                }
            }
            .subscribe(
                { objectId -> logger.debug("Аудит зарегистрирован для объекта: $objectId") },
                { error -> logger.error("Ошибка службы аудита: ${error.message}", error) }
            )

        businessService.getBusinessEvent2Stream()
            .doOnSubscribe { logger.info("Служба аудита подписалась на поток BusinessEvent2") }
            .flatMap { event ->
                Mono.fromCallable {
                    logger.info("AUDIT LOG 2: ${event.payload} at ${event.time}")
                    event.payload.objectId
                }
            }
            .subscribe(
                { objectId -> logger.debug("Audit2 зарегистрирован для объекта: $objectId") },
                { error -> logger.error("Ошибка службы аудита 2: ${error.message}", error) }
            )
    }

}
