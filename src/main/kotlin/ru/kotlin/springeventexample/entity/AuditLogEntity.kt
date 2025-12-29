package ru.kotlin.springeventexample.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("audit_logs")
data class AuditLogEntity(
    @Id
    val id: Long? = null,

    @Column("object_id")
    val objectId: Int,

    @Column("action_type")
    val actionType: String,

    @Column("event_type")
    val eventType: String,

    @Column("event_time")
    val eventTime: LocalDateTime,

    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column("source_service")
    val sourceService: String = "audit-service",

    @Column("metadata")
    val metadata: String? = null
) : Comparable<AuditLogEntity> {

    // Реализуем Comparable для сортировки
    override fun compareTo(other: AuditLogEntity): Int {
        return this.createdAt.compareTo(other.createdAt)
    }

    companion object {
        // Компаратор для обратной сортировки (новые сначала)
        val DESCENDING_COMPARATOR = Comparator<AuditLogEntity> { a, b ->
            b.createdAt.compareTo(a.createdAt)
        }

        // Компаратор для прямой сортировки (старые сначала)
        val ASCENDING_COMPARATOR = Comparator<AuditLogEntity> { a, b ->
            a.createdAt.compareTo(b.createdAt)
        }
    }

}
