package ru.spliterash.lettuceHelper.lock.impl

import kotlinx.coroutines.Job
import ru.spliterash.lettuceHelper.lock.base.DistributedLock

class LettuceDistributedLock internal constructor(
    override val id: String,
    private val callback: ServiceCallback,
    private val randomKey: ByteArray,
) : DistributedLock {
    private var extensionJob: Job? = null
    override suspend fun isLocked(): Boolean {
        return callback.isLocked(id)
    }

    override suspend fun tryLock(): Boolean {
        val result = callback.tryLock(id, randomKey)
        if (result)
            startExtensionJob()

        return result
    }

    override suspend fun lock() {
        callback.lock(id, randomKey)
        startExtensionJob()
    }

    override suspend fun unlock() {
        extensionJob?.cancel()
        callback.unlock(id, randomKey)
    }

    override fun release() {
        extensionJob?.cancel()
        callback.release(this)
    }

    private fun startExtensionJob() {
        extensionJob?.cancel()
        extensionJob = callback.startExtension(id, randomKey)
    }
}