package ru.kotlin.springeventexample.controller

import org.springframework.web.bind.annotation.*
import ru.kotlin.springeventexample.service.BusinessService

@RestController
class BusinessController (
    private val businessService: BusinessService
){

    @PostMapping("/create/{id}")
    fun create(@PathVariable id:Int): String{
        businessService.create0bject(id)
        return "Объект с id = $id создан в системе"
    }

    @PutMapping("/update/{id}")
    fun update(@PathVariable id:Int): String{
        businessService.update0bject(id)
        return "Объект с id = $id обновлен в системе"
    }

    @DeleteMapping("/delete/{id}")
    fun delete(@PathVariable id:Int): String{
        businessService.delete0bject(id)
        return "Объект с id = $id удален из системы"
    }

}