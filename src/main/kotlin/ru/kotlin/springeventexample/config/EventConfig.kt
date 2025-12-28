package ru.kotlin.springeventexample.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Sinks
import ru.kotlin.springeventexample.event.BusinessEvent
import ru.kotlin.springeventexample.event.BusinessEvent2

@Configuration
class EventConfig {

    @Bean
    fun businessEventSink(): Sinks.Many<BusinessEvent> {
        return Sinks.many()
            .multicast()
            .onBackpressureBuffer(1000) // Ограничиваем буфер
    }

    @Bean
    fun businessEvent2Sink(): Sinks.Many<BusinessEvent2> {
        return Sinks.many()
            .multicast()
            .onBackpressureBuffer(1000)
    }
}