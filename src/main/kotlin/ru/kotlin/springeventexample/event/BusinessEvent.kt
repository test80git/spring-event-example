package ru.kotlin.springeventexample.event

import org.springframework.context.ApplicationEvent
import java.time.LocalDateTime

data class BusinessEvent(
    val sourceObject: Any,
    val payload: ActionInfo,
) : ApplicationEvent(sourceObject)


data class ActionInfo(
    val objectId: Int,
    val actionType: ActionType,
)


enum class ActionType {
    CREATE,
    UPDATE,
    DELETE,
}

data class BusinessEvent2(
    val sourceObject: Any,
    val payload: ActionInfo,
    val time: LocalDateTime
) : ApplicationEvent(sourceObject)
