package ru.coderoom.autograding

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AutogradingServiceApplication

fun main(args: Array<String>) {
    runApplication<AutogradingServiceApplication>(*args)
}
