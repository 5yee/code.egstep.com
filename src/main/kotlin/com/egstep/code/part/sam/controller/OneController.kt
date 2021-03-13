package com.egstep.code.part.sam.controller

import com.egstep.code.part.sam.service.OneService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/one")
class OneController {

    @Autowired lateinit var service: OneService

    @GetMapping
    fun main(): String {
        return "alive"
    }

    @GetMapping("/abc")
    fun abc(): String {
        val result = service.one()

        return result
    }
}
