package ru.spliterash.lettuceHelper.lock.impl

import ru.spliterash.lettuceHelper.lock.base.DistributedLock

class LettuceDistributedLock internal constructor(
    override val id: String,
    private val callback: ServiceCallback,
    private val randomKey: ByteArray,
) : DistributedLock {

    override suspend fun isLocked(): Boolean {
        return callback.isLocked(id)
    }

    override suspend fun tryLock(): Boolean {
        return callback.tryLock(id, randomKey)
    }

    override suspend fun lock() {
        callback.lock(id, randomKey)
    }

    override suspend fun unlock() {
        callback.unlock(id, randomKey)
    }

    override fun release() {
        callback.release(this)
    }
}