package dev.michaelburgess.mymusic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class MyMusicApplication

fun main(args: Array<String>) {
    runApplication<MyMusicApplication>(*args)
}