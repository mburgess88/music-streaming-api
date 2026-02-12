package dev.michaelburgess.mymusic.repository

import dev.michaelburgess.mymusic.domain.Category
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CategoryRepository {
    fun save(category: Category): Mono<Void>
    fun get(id: String): Mono<Category>
    fun getAll(): Flux<Category>
}
