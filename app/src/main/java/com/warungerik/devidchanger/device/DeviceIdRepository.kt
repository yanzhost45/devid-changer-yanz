package com.warungerik.devidchanger.device

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceIdRepository {

    private val ids = mutableListOf<String>()

    private var currentIndex = -1

    suspend fun loadFromUri(
        context: Context,
        uri: Uri
    ): Result<Int> = withContext(Dispatchers.IO) {

        try {

            val loadedIds =
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { reader ->

                        reader.readLines()
                            .asSequence()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .toList()
                    }
                    ?: return@withContext Result.failure(
                        Exception("Gagal membuka file")
                    )

            if (loadedIds.isEmpty()) {

                return@withContext Result.failure(
                    Exception("File tidak berisi Device ID")
                )
            }

            synchronized(ids) {

                ids.clear()

                ids.addAll(loadedIds)

                currentIndex = 0
            }

            Result.success(
                loadedIds.size
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    fun current(): String? =
        synchronized(ids) {

            if (
                currentIndex < 0 ||
                currentIndex >= ids.size
            ) {
                null
            } else {
                ids[currentIndex]
            }
        }

    fun next(): String? =
        synchronized(ids) {

            if (ids.isEmpty()) {
                return@synchronized null
            }

            currentIndex =
                (currentIndex + 1) % ids.size

            ids[currentIndex]
        }

    fun previous(): String? =
        synchronized(ids) {

            if (ids.isEmpty()) {
                return@synchronized null
            }

            currentIndex =
                if (currentIndex <= 0) {
                    ids.lastIndex
                } else {
                    currentIndex - 1
                }

            ids[currentIndex]
        }

    fun position(): Int =
        synchronized(ids) {

            if (ids.isEmpty()) {
                0
            } else {
                currentIndex + 1
            }
        }

    fun size(): Int =
        synchronized(ids) {
            ids.size
        }

    fun isEmpty(): Boolean =
        synchronized(ids) {
            ids.isEmpty()
        }

    fun clear() =
        synchronized(ids) {

            ids.clear()

            currentIndex = -1
        }
}
