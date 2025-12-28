package ru.kotlin.springeventexample.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Configuration
class ReactiveConfig {

    @Bean
    fun retryPolicy(): Retry = Retry.backoff(3, Duration.ofSeconds(1))
        .maxBackoff(Duration.ofSeconds(10))
        .jitter(0.5)
        .doBeforeRetry { retrySignal ->
            LoggerFactory.getLogger(ReactiveConfig::class.java)
                .warn("Повторная попытка после ошибки: ${retrySignal.failure().message}")
        }
        .onRetryExhaustedThrow { retryBackoffSpec, retrySignal ->
            LoggerFactory.getLogger(ReactiveConfig::class.java)
                .error("Повторить попытку исчерпано после ${retrySignal.totalRetries()} попытка")
            retrySignal.failure()
        }
}

// Глобальный обработчик ошибок
@Configuration
class GlobalErrorHandler {

    fun handleError(throwable: Throwable): Mono<ServerResponse> {
        LoggerFactory.getLogger(GlobalErrorHandler::class.java)
            .error("Необработанная ошибка: ${throwable.message}", throwable)

        return ServerResponse.badRequest()
            .bodyValue(mapOf("error" to "Internal server error"))
    }
}