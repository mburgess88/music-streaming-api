package dev.michaelburgess.mymusic.service

import dev.michaelburgess.mymusic.domain.Category
import dev.michaelburgess.mymusic.repository.CategoryRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {
    fun getAllCategories(): Flux<Category> = categoryRepository.getAll()
    fun getCategory(id: String): Mono<Category> = categoryRepository.get(id)
    fun saveCategory(category: Category): Mono<Void> = categoryRepository.save(category)
}
