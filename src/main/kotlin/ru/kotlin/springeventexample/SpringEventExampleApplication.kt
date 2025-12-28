package ru.kotlin.springeventexample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync


@SpringBootApplication
@EnableAsync
class SpringEventExampleApplication

fun main(args: Array<String>) {
    runApplication<SpringEventExampleApplication>(*args)
}
