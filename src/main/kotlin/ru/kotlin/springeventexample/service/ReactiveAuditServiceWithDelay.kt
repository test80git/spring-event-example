package ru.kotlin.springeventexample.service

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class ReactiveAuditServiceWithDelay(
    private val businessService: ReactiveBusinessService
) {
    private val logger = LoggerFactory.getLogger(ReactiveAuditServiceWithDelay::class.java)

    // Имитация БД в памяти
    private val auditLogs = ConcurrentHashMap<Long, AuditLog>()
    private val idGenerator = AtomicLong(1)

    // Статистика задержек (в мс)
    private val minDelay = 50L
    private val maxDelay = 300L
    private val averageDelay = 150L

    data class AuditLog(
        val id: Long,
        val objectId: Int,
        val actionType: String,
        val eventType: String,
        val timestamp: LocalDateTime,
        val processingTime: Long // время обработки в мс
    )

    @PostConstruct
    fun init() {
        logger.info("Инициализация AuditServiceWithDelay с задержками (${minDelay}-${maxDelay}ms)...")

        // Создаем отдельный поток для мониторинга
        Thread {
            while (true) {
                Thread.sleep(30000) // каждые 30 секунд
                val size = auditLogs.size
                val avgProcessingTime = if (size > 0) {
                    auditLogs.values.map { it.processingTime }.average()
                } else 0.0

                logger.info(
                    """
                    |Статистика имитации БД:
                    |  Всего записей: $size
                    |  Среднее время обработки: ${"%.2f".format(avgProcessingTime)} мс
                """.trimMargin()
                )
            }
        }.apply {
            isDaemon = true
            start()
        }

        // Обработка BusinessEvent с задержкой
        businessService.getBusinessEventStream()
            .doOnSubscribe {
                logger.info("AuditServiceWithDelay подписан на поток событий")
            }
            .onBackpressureBuffer(10000) // Большой буфер для имитации нагрузки
            .flatMap({ event ->
                processWithDelay(event, "BusinessEvent")
                    .onErrorResume { error ->
                        logger.error("Ошибка обработки: ${error.message}")
                        Mono.empty()
                    }
            }, 10) // 10 параллельных обработок
            .subscribe(
                { auditLog ->
                    logger.debug("Обработано: ${auditLog.objectId} за ${auditLog.processingTime}мс")
                },
                { error ->
                    logger.error("Фатальная ошибка: ${error.message}", error)
                }
            )

        // Обработка BusinessEvent2
        businessService.getBusinessEvent2Stream()
            .onBackpressureBuffer(5000)
            .flatMap({ event ->
                processWithDelay(event, "BusinessEvent2")
                    .onErrorResume { error ->
                        logger.error("Ошибка обработки BusinessEvent2: ${error.message}")
                        Mono.empty()
                    }
            }, 5)
            .subscribe(
                { auditLog -> logger.debug("Обработано BusinessEvent2: ${auditLog.objectId}") },
                { error -> logger.error("Фатальная ошибка BusinessEvent2: ${error.message}") }
            )
    }

    private fun processWithDelay(event: BusinessEvent, eventType: String): Mono<AuditLog> = mono {
        val startTime = System.currentTimeMillis()

        // Имитация задержки БД (случайное значение в диапазоне)
        val delay = (minDelay..maxDelay).random()
        logger.debug("Имитация БД: задержка ${delay}мс для события ${event.payload}")

        // delay вместо Thread.sleep для корутин
        delay(delay)

        // "Сохраняем" в "БД"
        val auditLog = AuditLog(
            id = idGenerator.getAndIncrement(),
            objectId = event.payload.objectId,
            actionType = event.payload.actionType.name,
            eventType = eventType,
            timestamp = LocalDateTime.now(),
            processingTime = System.currentTimeMillis() - startTime
        )

        auditLogs[auditLog.id] = auditLog

        logger.info("Аудит сохранен: ${event.payload} (заняло ${auditLog.processingTime}мс)")
        auditLog
    }

    private fun processWithDelay(event: BusinessEvent2, eventType: String): Mono<AuditLog> = mono {
        val startTime = System.currentTimeMillis()

        // Для BusinessEvent2 делаем дольше
        val delay = (200L..500L).random()
        delay(delay)

        val auditLog = AuditLog(
            id = idGenerator.getAndIncrement(),
            objectId = event.payload.objectId,
            actionType = event.payload.actionType.name,
            eventType = eventType,
            timestamp = event.time,
            processingTime = System.currentTimeMillis() - startTime
        )

        auditLogs[auditLog.id] = auditLog
        logger.info("Аудит2 сохранен: ${event.payload} (заняло ${auditLog.processingTime}мс)")
        auditLog
    }

    // Методы для тестирования
    suspend fun getAllAuditLogs(): List<AuditLog> {
        return auditLogs.values.toList()
    }

    suspend fun getAuditLogsByObjectId(objectId: Int): List<AuditLog> {
        // Имитация задержки запроса
        delay(100)
        return auditLogs.values.filter { it.objectId == objectId }
    }

    suspend fun clearAuditLogs() {
        delay(50) // Имитация задержки очистки
        auditLogs.clear()
        logger.info("Аудит-логи очищены")
    }

    // Имитация медленного запроса
    suspend fun simulateSlowQuery(objectId: Int, delaySeconds: Long = 5): List<AuditLog> {
        logger.warn("Имитация медленного запроса: $delaySeconds секунд")
        delay(delaySeconds * 1000)
        return getAuditLogsByObjectId(objectId)
    }
}