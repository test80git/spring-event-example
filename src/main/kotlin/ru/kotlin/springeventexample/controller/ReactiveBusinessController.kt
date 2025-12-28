package ru.kotlin.springeventexample.controller

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.kotlin.springeventexample.service.ReactiveBusinessService
import ru.kotlin.springeventexample.service.ReactiveSMSService
import java.time.Duration

@RestController
@RequestMapping("/api")
class ReactiveBusinessController(
    private val businessService: ReactiveBusinessService,
    private val smsService: ReactiveSMSService
) {

    @PostMapping("/create/{id}")
    suspend fun create(@PathVariable id: Int): Mono<String> {
        return businessService.createObject(id)
            .onErrorMap { error ->
                ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось создать объект: ${error.message}",
                    error
                )
            }
    }

    @PutMapping("/update/{id}")
    suspend fun update(@PathVariable id: Int): Mono<String> {
        return businessService.updateObject(id)
            .onErrorMap { error ->
                ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось обновить объект: ${error.message}",
                    error
                )
            }
    }

    @DeleteMapping("/delete/{id}")
    suspend fun delete(@PathVariable id: Int): Mono<String> {
        return businessService.deleteObject(id)
            .onErrorMap { error ->
                ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось удалить объект: ${error.message}",
                    error
                )
            }
    }

    // SSE endpoint для мониторинга событий в реальном времени
    @GetMapping(value = ["/events/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamEvents(): Flux<String> {
        return businessService.getBusinessEventStream()
            .map { event ->
                "data: ${event.payload}\n\n"
            }
            .mergeWith(
                businessService.getBusinessEvent2Stream()
                    .map { event ->
                        "data: ${event.payload} at ${event.time}\n\n"
                    }
            )
            .doOnSubscribe { println("Клиент подписался на поток событий") }
            .doOnCancel { println("Клиент отписался от потока событий") }
    }

    // Мониторинг SMS событий
    @GetMapping(value = ["/sms/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamSmsEvents(): Flux<String> {
        return smsService.getBusinessEventFlux()
            .map { event ->
                "data: SMS queued for ${event.payload}\n\n"
            }
            .delayElements(Duration.ofMillis(500)) // Замедляем для демонстрации
    }

    // Bulk операции
    @PostMapping("/bulk/create")
    suspend fun bulkCreate(@RequestBody ids: List<Int>): Mono<Map<String, Any>> {
        val results = ids.map { id ->
            businessService.createObject(id)
                .map { id to "created" }
                .onErrorResume { error ->
                    Mono.just(id to "failed: ${error.message}")
                }
        }

        return Flux.fromIterable(results)
            .flatMap { it }
            .collectList()
            .map { resultList ->
                mapOf(
                    "total" to ids.size,
                    "success" to resultList.count { it.second == "created" },
                    "failed" to resultList.count { it.second.startsWith("failed") },
                    "results" to resultList.associate { it.first.toString() to it.second }
                )
            }
    }
}