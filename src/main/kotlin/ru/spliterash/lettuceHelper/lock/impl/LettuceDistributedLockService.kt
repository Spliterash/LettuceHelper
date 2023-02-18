package ru.spliterash.lettuceHelper.lock.impl

import io.lettuce.core.ScriptOutputType
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import ru.spliterash.lettuceHelper.LettuceModulesByteExecutorService
import ru.spliterash.lettuceHelper.LettucePubSubService
import ru.spliterash.lettuceHelper.base.pubsub.LettuceSubscribe
import ru.spliterash.lettuceHelper.extensions.executeAsync
import ru.spliterash.lettuceHelper.lock.base.DistributedLock
import ru.spliterash.lettuceHelper.lock.base.DistributedLockService
import ru.spliterash.lettuceHelper.lock.exceptions.UnlockSomeoneElseLockException
import java.util.*
import kotlin.coroutines.resume

private const val ATOMIC_DELETE_SCRIPT =
    "if redis.call('get',KEYS[1])==ARGV[1]then redis.call('del',KEYS[1])return true else return false end"

class LettuceDistributedLockService(
    private val startPath: String,
    private val executor: LettuceModulesByteExecutorService,
    private val pubSubService: LettucePubSubService
) : DistributedLockService {
    private val random = Random()
    private val locks = Collections.synchronizedMap(hashMapOf<String, DistributedLock>())
    private val callback = ServiceCallbackImpl()
    private var subscribe: LettuceSubscribe? = null
    private val unlockListeners = hashMapOf<String, MutableList<Runnable>>()

    private fun lockPath(id: String): String = "$startPath:ids:$id"
    private val channelPath: String = "$startPath:channel"

    /**
     * Создать объект для распределённых блокировок
     *
     * Создание объекта не означает моментальную блокировку, уже после создания можете делать свои дела
     *
     * Нельзя создать 2 блокировки с 1 ID, те внутренние блокировки сами разруливайте
     */
    override fun createDistributedLock(id: String): DistributedLock {
        if (locks.containsKey(id))
            throw IllegalArgumentException("Lock with id $id already aquired by this service")

        val key = randomString()
        return LettuceDistributedLock(id, callback, key).apply {
            locks[id] = this
        }
    }


    private fun destroyLock(lock: LettuceDistributedLock) {
        locks.remove(lock.id, lock)
    }

    private suspend fun isLocked(id: String) = executor.executeAsync {
        exists(lockPath(id)).await() > 0
    }

    private suspend fun tryLock(id: String, key: ByteArray): Boolean = executor.executeAsync {
        val path = lockPath(id)

        setnx(path, key).await()
    }

    private suspend fun lock(id: String, key: ByteArray): Unit = coroutineScope {
        if (tryLock(id, key))
            return@coroutineScope

        suspendCancellableCoroutine { cont ->
            var run = false
            addUnlockListener(id) {
                if (run)
                    return@addUnlockListener
                run = true
                cont.resume(Unit)
            }
            cont.invokeOnCancellation {
                // Проигнорим уведомление, так как там уже пофигу
                run = true
            }
            // На всякий случай проверим ещё раз, так как возможно разблокировка произошла точно в момент добавления
            manualCheckLockAndFireEventIfUnlock(id)
        }

        // Ah shit, here we go again
        lock(id, key)
    }

    private fun manualCheckLockAndFireEventIfUnlock(id: String) = CoroutineScope(Dispatchers.IO).launch {
        if (!isLocked(id))
            runUnlockListeners(id)
    }

    private suspend fun unlock(id: String, key: ByteArray) {
        val path = lockPath(id)
        val result = executor.executeAsync {
            eval<Boolean>(ATOMIC_DELETE_SCRIPT, ScriptOutputType.BOOLEAN, arrayOf(path), key).await()
        }

        if (!result)
            throw UnlockSomeoneElseLockException()

        publishUnlockMessage(id)
    }

    private fun randomString(): ByteArray {
        val buf = ByteArray(12)
        random.nextBytes(buf)

        return buf
    }

    override suspend fun init() {
        subscribe = pubSubService.subscribe(channelPath) { message ->
            val unlockedId = message.decodeToString()
            runUnlockListeners(unlockedId)
        }
    }

    override suspend fun destroy() {
        subscribe?.unsubscribe()
        unlockListeners.clear()
        subscribe = null
    }

    private fun addUnlockListener(id: String, run: Runnable) {
        unlockListeners.computeIfAbsent(id) { arrayListOf() }.add(run)
    }

    private suspend fun publishUnlockMessage(id: String) = executor.executeAsync {
        publish(channelPath, id.encodeToByteArray()).await()
    }

    private fun runUnlockListeners(id: String) {
        val listeners = unlockListeners.remove(id) ?: return

        for (listener in listeners) {
            listener.run()
        }
    }


    private inner class ServiceCallbackImpl : ServiceCallback {
        override suspend fun tryLock(id: String, randomKey: ByteArray) =
            this@LettuceDistributedLockService.tryLock(id, randomKey)

        override suspend fun lock(id: String, randomKey: ByteArray) =
            this@LettuceDistributedLockService.lock(id, randomKey)

        override suspend fun unlock(id: String, randomKey: ByteArray) =
            this@LettuceDistributedLockService.unlock(id, randomKey)

        override suspend fun isLocked(id: String) =
            this@LettuceDistributedLockService.isLocked(id)

        override fun release(lock: LettuceDistributedLock) = destroyLock(lock)
    }
}