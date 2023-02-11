package ru.spliterash.lettuceHelper.lock.base

interface DistributedLockService {
    /**
     * Создать объект для распределённых блокировок
     *
     * Создание объекта не означает моментальную блокировку, уже после создания можете делать свои дела
     *
     * Нельзя создать 2 блокировки с 1 ID, те внутренние блокировки сами разруливайте
     */
    fun createDistributedLock(id: String): DistributedLock

    suspend fun init()

    suspend fun destroy()
}