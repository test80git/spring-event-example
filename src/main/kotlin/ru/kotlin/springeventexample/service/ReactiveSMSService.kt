package ru.kotlin.springeventexample.service

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.kotlin.springeventexample.entity.AuditLogEntity
import ru.kotlin.springeventexample.event.ActionType
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2
import ru.kotlin.springeventexample.repository.AuditLogRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

@Service
class ReactiveSMSService(
    private val businessService: ReactiveBusinessService,
    private val auditLogRepository: AuditLogRepository
) {
    private val logger = LoggerFactory.getLogger(ReactiveSMSService::class.java)

    // Статистика для мониторинга (используем Atomic для thread-safety)
    private val processedCount = AtomicLong(0L)
    private val errorCount = AtomicLong(0L)
    private var lastProcessedTime = LocalDateTime.now()

    @PostConstruct
    fun init() {
        // Подписываемся на поток событий BusinessEvent
        businessService.getBusinessEventStream()
            .doOnSubscribe { logger.info("SMS-сервис подписался на поток BusinessEvent") }
//            .onBackpressureBuffer(200) // Добавляем буфер на 200 событий
            .flatMap { event ->
                saveAuditLog(event, "BusinessEvent")
                    .doOnSuccess {
                        processedCount.incrementAndGet()
                        lastProcessedTime = LocalDateTime.now()
                    }
                    .doOnError { error ->
                        errorCount.incrementAndGet()
                        logger.error("Ошибка сохранения смс: ${error.message}", error)
                    }
                    .onErrorResume { error ->
                        logger.warn("Пропускаем событие из-за ошибки: ${error.message}")
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
                saveAuditLog(event, "BusinessEvent")
                    .doOnSuccess {
                        processedCount.incrementAndGet()
                        lastProcessedTime = LocalDateTime.now()
                    }
                    .doOnError { error ->
                        errorCount.incrementAndGet()
                        logger.error("Ошибка сохранения смс2: ${error.message}", error)
                    }
                    .onErrorResume { error ->
                        logger.warn("Пропускаем событие из-за ошибки: ${error.message}")
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


    @Transactional
    fun saveAuditLog(event: BusinessEvent, eventType: String): Mono<AuditLogEntity> {
        return Mono.fromCallable {
            AuditLogEntity(
                objectId = event.payload.objectId,
                actionType = event.payload.actionType.name,
                eventType = eventType,
                eventTime = LocalDateTime.now(),
                sourceService = this.javaClass.simpleName,
                metadata = "source: ${event.sourceObject::class.simpleName}"
            )
        }.flatMap { auditLog ->
            auditLogRepository.save(auditLog)
                .doOnSuccess { saved ->
                    logger.debug("СМС сохранен в БД: ID=${saved.id}, object=${saved.objectId}")
                }
                .doOnError { error ->
                    logger.error("Ошибка сохранения: ${error.message}")
                }
        }
    }

    @Transactional
    fun saveAuditLog(event: BusinessEvent2, eventType: String): Mono<AuditLogEntity> {
        return Mono.fromCallable {
            AuditLogEntity(
                objectId = event.payload.objectId,
                actionType = event.payload.actionType.name,
                eventType = eventType,
                eventTime = event.time,
                sourceService = this.javaClass.simpleName,
                metadata = "source: ${event.sourceObject::class.simpleName}, time=${event.time}"
            )
        }.flatMap { auditLog ->
            auditLogRepository.save(auditLog)
                .doOnError { error ->
                    logger.error("Ошибка сохранения BusinessEvent2: ${error.message}")
                }
        }
    }
}