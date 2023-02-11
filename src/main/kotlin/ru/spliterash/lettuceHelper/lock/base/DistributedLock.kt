package ru.spliterash.lettuceHelper.lock.base

interface DistributedLock {
    val id: String

    suspend fun isLocked(): Boolean

    /**
     * Попытаться заблокировать ресурс
     *
     * true если успешно заблокано, можно продолжать действия
     * false если ресурс уже заблокирован
     */
    suspend fun tryLock(): Boolean

    /**
     * Заблокировать ресурс, если уже заблокировано, продолжит выполнение только после освобождения
     */
    suspend fun lock()
    suspend fun unlock()

    /**
     * Этот объект больше не будет использоваться, отпустить его
     */
    fun release()
}