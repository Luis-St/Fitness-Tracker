package net.luis.tracker.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import net.luis.tracker.data.local.dao.ExerciseDao
import net.luis.tracker.domain.mapper.toDomain
import net.luis.tracker.domain.mapper.toEntity
import net.luis.tracker.domain.model.Exercise

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

	fun getAllActive(): Flow<List<Exercise>> =
		exerciseDao.getAllActive()
			.map { list -> list.map { it.toDomain() } }
			.flowOn(Dispatchers.Default)

	fun getByCategory(categoryId: Long): Flow<List<Exercise>> =
		exerciseDao.getByCategory(categoryId)
			.map { list -> list.map { it.toDomain() } }
			.flowOn(Dispatchers.Default)

	suspend fun getById(id: Long): Exercise? =
		exerciseDao.getById(id)?.toDomain()

	suspend fun insert(exercise: Exercise): Long =
		exerciseDao.insert(exercise.toEntity())

	suspend fun update(exercise: Exercise) =
		exerciseDao.update(exercise.toEntity())

	suspend fun softDelete(id: Long) =
		exerciseDao.softDelete(id)
}
