package ru.kotlin.springeventexample.event

import org.springframework.context.ApplicationEvent
import java.time.LocalDateTime

data class BusinessEvent(
    val source0bject: Any,
    val payload: ActionInfo,
) : ApplicationEvent(source0bject)


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
    val source0bject: Any,
    val payload: ActionInfo,
    val time: LocalDateTime
) : ApplicationEvent(source0bject)