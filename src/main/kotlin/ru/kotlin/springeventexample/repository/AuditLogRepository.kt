package ru.kotlin.springeventexample.repository

import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import ru.kotlin.springeventexample.entity.AuditLogEntity
import java.time.LocalDateTime

@Repository
interface AuditLogRepository : ReactiveCrudRepository<AuditLogEntity, Long> {

    fun findByObjectId(objectId: Int): Flux<AuditLogEntity>

    fun findByActionType(actionType: String): Flux<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE event_time >= :from AND event_time <= :to")
    fun findByEventTimeBetween(from: LocalDateTime, to: LocalDateTime): Flux<AuditLogEntity>

    // Метод для сортировки по времени создания
    fun findAllByOrderByCreatedAtDesc(): Flux<AuditLogEntity>

    // Или с использованием Sort
    fun findAllBy(sort: Sort): Flux<AuditLogEntity>

    // С ограничением по количеству
    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit")
    fun findRecentLogs(limit: Int): Flux<AuditLogEntity>
}