package ru.spliterash.lettuceHelper.lock.impl

import kotlinx.coroutines.Job

internal interface ServiceCallback {
    suspend fun tryLock(id: String, randomKey: ByteArray): Boolean
    suspend fun lock(id: String, randomKey: ByteArray)
    suspend fun unlock(id: String, randomKey: ByteArray)
    suspend fun isLocked(id: String): Boolean

    fun startExtension(id: String, randomKey: ByteArray): Job
    fun release(lock: LettuceDistributedLock)

}
