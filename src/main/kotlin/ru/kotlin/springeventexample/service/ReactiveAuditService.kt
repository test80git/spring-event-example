package ru.kotlin.springeventexample.service

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.kotlin.springeventexample.entity.AuditLogEntity
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2
import ru.kotlin.springeventexample.repository.AuditLogRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

@Service
class ReactiveAuditService(
    private val businessService: ReactiveBusinessService,
    private val auditLogRepository: AuditLogRepository
) {
    private val logger = LoggerFactory.getLogger(ReactiveAuditService::class.java)

    // Статистика для мониторинга (используем Atomic для thread-safety)
    private val processedCount = AtomicLong(0L)
    private val errorCount = AtomicLong(0L)
    private var lastProcessedTime = LocalDateTime.now()

    @PostConstruct
    fun init() {
        logger.info("Инициализация ReactiveAuditService...")

        // Мониторинг статистики каждые 30 секунд
        Flux.interval(Duration.ofSeconds(30))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                val count = processedCount.get()
                val errors = errorCount.get()

                logger.info(
                    """
                    |Статистика AuditService:
                    |  Обработано событий: $count
                    |  Ошибок: $errors
                    |  Скорость: ${count / 30} событий/сек
                    |  Последняя обработка: $lastProcessedTime
                """.trimMargin()
                )
            }

        // Обработка BusinessEvent
        businessService.getBusinessEventStream()
            .doOnSubscribe {
                logger.info("AuditService подписан на BusinessEvent поток")
            }
            .doOnNext { event ->
                logger.debug("Получено событие для аудита: ${event.payload}")
            }
            .flatMap { event ->
                saveAuditLog(event, "BusinessEvent")
                    .doOnSuccess {
                        processedCount.incrementAndGet()
                        lastProcessedTime = LocalDateTime.now()
                    }
                    .doOnError { error ->
                        errorCount.incrementAndGet()
                        logger.error("Ошибка сохранения аудита: ${error.message}", error)
                    }
                    .onErrorResume { error ->
                        logger.warn("Пропускаем событие из-за ошибки: ${error.message}")
                        Mono.empty()
                    }
            }
            .subscribe(
                { savedLog -> logger.debug("Аудит сохранен: ${savedLog.objectId}") },
                { error -> logger.error("Критическая ошибка AuditService: ${error.message}", error) }
            )

        // Обработка BusinessEvent2
        businessService.getBusinessEvent2Stream()
            .doOnSubscribe {
                logger.info("AuditService подписан на BusinessEvent2 поток")
            }
            .flatMap { event ->
                saveAuditLog(event, "BusinessEvent2")
                    .doOnSuccess {
                        processedCount.incrementAndGet()
                        lastProcessedTime = LocalDateTime.now()
                    }
                    .onErrorResume { error ->
                        errorCount.incrementAndGet()
                        logger.warn("Ошибка сохранения BusinessEvent2: ${error.message}")
                        Mono.empty()
                    }
            }
            .subscribe(
                { savedLog -> logger.debug("Аудит2 сохранен: ${savedLog.objectId}") },
                { error -> logger.error("Критическая ошибка AuditService2: ${error.message}", error) }
            )
    }

    // Исправление 1: Убираем suspend и используем flatMap напрямую
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
                    logger.debug("Аудит сохранен в БД: ID=${saved.id}, object=${saved.objectId}")
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

    // Пакетное сохранение для оптимизации
    @Transactional
    fun saveAuditLogsBatch(events: List<BusinessEvent>): Mono<List<AuditLogEntity>> {
        val auditLogs = events.map { event ->
            AuditLogEntity(
                objectId = event.payload.objectId,
                actionType = event.payload.actionType.name,
                eventType = "BusinessEvent",
                eventTime = LocalDateTime.now()
            )
        }

        return auditLogRepository.saveAll(auditLogs)
            .collectList()
            .doOnSuccess { savedLogs ->
                logger.info("Пакетно сохранено ${savedLogs.size} записей аудита")
            }
            .doOnError { error ->
                logger.error("Ошибка пакетного сохранения: ${error.message}")
            }
    }

    // Методы для запросов
    fun getAuditLogsByObjectId(objectId: Int): Flux<AuditLogEntity> {
        return auditLogRepository.findByObjectId(objectId)
            .doOnSubscribe { logger.debug("Запрос аудит-логов для objectId: $objectId") }
            .doOnError { error ->
                logger.error("Ошибка запроса логов по objectId: ${error.message}")
            }
    }

    // Исправление 2: Правильная сортировка
    fun getRecentAuditLogs(limit: Int = 100): Flux<AuditLogEntity> {
        return auditLogRepository.findAll()
            .take(limit.toLong())
            .sort { a, b ->
                // Сортируем по времени создания (новые сначала)
                b.createdAt.compareTo(a.createdAt )
            }
    }

    // Альтернативный вариант сортировки через репозиторий
    fun getRecentAuditLogsSorted(limit: Int = 100): Flux<AuditLogEntity> {
        return Flux.defer {
            auditLogRepository.findAllByOrderByCreatedAtDesc()
                .take(limit.toLong())
        }
    }

    // Статистика
    fun getStatistics(): Map<String, Any> {
        val count = processedCount.get()
        return mapOf(
            "processedCount" to count,
            "errorCount" to errorCount.get(),
            "lastProcessedTime" to lastProcessedTime,
            "throughput" to if (count > 0) count / 30 else 0 // событий в секунду
        )
    }

    // Метод с корутинами для вызова из suspend функций
    suspend fun saveAuditLogSuspend(event: BusinessEvent, eventType: String): AuditLogEntity? {
        return saveAuditLog(event, eventType).awaitSingleOrNull()
    }

    suspend fun saveAuditLogSuspend(event: BusinessEvent2, eventType: String): AuditLogEntity? {
        return saveAuditLog(event, eventType).awaitSingleOrNull()
    }
}