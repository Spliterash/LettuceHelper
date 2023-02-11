package ru.spliterash.lettuceHelper.lock.ext

import ru.spliterash.lettuceHelper.lock.base.DistributedLock
import ru.spliterash.lettuceHelper.lock.impl.LettuceDistributedLockService

suspend inline fun <T> DistributedLock.withLock(block: () -> T) {
    lock()
    try {
        block()
    } finally {
        unlock()
    }
}

suspend inline fun <T> LettuceDistributedLockService.withLock(id: String, block: () -> T) {
    val lock = this.createDistributedLock(id)

    try {
        lock.withLock(block)
    } finally {
        lock.release()
    }
}