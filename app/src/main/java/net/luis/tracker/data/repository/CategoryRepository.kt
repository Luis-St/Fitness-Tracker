package net.luis.tracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.luis.tracker.data.local.dao.CategoryDao
import net.luis.tracker.domain.mapper.toDomain
import net.luis.tracker.domain.mapper.toEntity
import net.luis.tracker.domain.model.Category

class CategoryRepository(private val categoryDao: CategoryDao) {

	fun getAll(): Flow<List<Category>> =
		categoryDao.getAll().map { entities -> entities.map { it.toDomain() } }

	suspend fun getById(id: Long): Category? =
		categoryDao.getById(id)?.toDomain()

	suspend fun insert(category: Category): Long =
		categoryDao.insert(category.toEntity())

	suspend fun update(category: Category) =
		categoryDao.update(category.toEntity())

	suspend fun delete(category: Category) =
		categoryDao.delete(category.toEntity())
}
