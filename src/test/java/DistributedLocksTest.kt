import com.redis.lettucemod.RedisModulesClient
import io.lettuce.core.RedisURI
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import ru.spliterash.lettuceHelper.LettuceModulesByteExecutorService
import ru.spliterash.lettuceHelper.LettucePubSubService
import ru.spliterash.lettuceHelper.extensions.executeAsync
import ru.spliterash.lettuceHelper.lock.base.DistributedLockService
import ru.spliterash.lettuceHelper.lock.impl.LettuceDistributedLockService
import kotlin.system.measureTimeMillis

// Tests not pass without redis server running

// I'm bad at unit testing
class DistributedLocksTest {

    companion object {
        lateinit var executor: LettuceModulesByteExecutorService
        lateinit var lockService: DistributedLockService
        lateinit var anotherLockService: DistributedLockService

        @BeforeAll
        @JvmStatic
        fun init() = runBlocking {
            val uri = RedisURI.builder()
                .withHost("localhost")
                .build()
            val client = RedisModulesClient.create(uri)

            executor = LettuceModulesByteExecutorService(client, uri)
            val pubsub = LettucePubSubService(client, uri)

            lockService = LettuceDistributedLockService("unit-tests", executor, pubsub)
            lockService.init()
            anotherLockService = LettuceDistributedLockService("unit-tests", executor, pubsub)
            anotherLockService.init()
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            runBlocking {
                lockService.destroy()
                anotherLockService.destroy()
                executor.executeAsync {
                    val keys = keys("unit-tests:*").await()
                    if (keys.isNotEmpty())
                        del(*keys.toTypedArray())
                }
            }
        }
    }

    @Test
    fun simpleTest() {
        val lock = lockService.createDistributedLock("test-lock")

        runBlocking {
            assert(lock.tryLock())
            delay(50)
            lock.unlock()
            lock.release()
        }
    }

    @Test
    fun alreadyLocked() = runBlocking {
        val id = "test-lock"
        val lock = lockService.createDistributedLock(id)
        assert(lock.tryLock())
        val alsoThisLock = anotherLockService.createDistributedLock(id)
        assert(!alsoThisLock.tryLock())

        launch {
            delay(100)
            lock.unlock()
        }
        withTimeout(250) {
            val lockAcquiredTime = measureTimeMillis {
                alsoThisLock.lock()
            }
            assert(lockAcquiredTime >= 100)
            alsoThisLock.unlock()
        }

        lock.release()
        alsoThisLock.release()
    }
}