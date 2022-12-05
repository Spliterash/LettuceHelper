package ru.spliterash.lettuceHelper.base.commands

import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.support.AsyncConnectionPoolSupport
import io.lettuce.core.support.BoundedAsyncPool
import io.lettuce.core.support.BoundedPoolConfig
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletionStage


class BaseLettuceExecutorService<T : StatefulConnection<*, *>>(
    private val factory: () -> CompletionStage<T>
) : LettuceExecutorService<T> {
    private val pool: BoundedAsyncPool<T>

    init {
        val config = BoundedPoolConfig.builder()
            .minIdle(2)
            .maxIdle(16)
            .maxTotal(64)
            .build()

        pool = AsyncConnectionPoolSupport.createBoundedObjectPool({ factory() }, config, false)
    }

    override suspend fun <R> execute(block: T.() -> R): R {
        val connection = pool.acquire().await()

        return try {
            block(connection)
        } finally {
            pool.release(connection)
        }
    }


    override suspend fun shutdown() {
        pool.closeAsync().await()
    }
}