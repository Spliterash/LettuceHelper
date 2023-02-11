package ru.spliterash.lettuceHelper.lock.impl

internal interface ServiceCallback {
    suspend fun tryLock(id: String, randomKey: ByteArray): Boolean
    suspend fun lock(id: String, randomKey: ByteArray)
    suspend fun unlock(id: String, randomKey: ByteArray)
    suspend fun isLocked(id: String): Boolean
    fun release(lock: LettuceDistributedLock)

}
