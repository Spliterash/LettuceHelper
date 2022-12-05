package ru.spliterash.lettuceHelper

import com.redis.lettucemod.RedisModulesClient
import com.redis.lettucemod.api.StatefulRedisModulesConnection
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.StringCodec
import ru.spliterash.lettuceHelper.base.commands.BaseLettuceExecutorService
import ru.spliterash.lettuceHelper.base.commands.LettuceExecutorService
import java.util.concurrent.CompletionStage


class LettuceModulesByteExecutorService(private val uri: RedisURI) :
    LettuceExecutorService<StatefulRedisModulesConnection<String, ByteArray>> {
    private val client: RedisModulesClient = RedisModulesClient.create(uri)


    private val pool = BaseLettuceExecutorService {
        @Suppress("UNCHECKED_CAST")
        client.connectAsync(
            StringCodec.UTF8,
            uri
        ) as CompletionStage<StatefulRedisModulesConnection<String, ByteArray>>
    }

    override suspend fun <R> execute(block: suspend StatefulRedisModulesConnection<String, ByteArray>.() -> R): R =
        pool.execute(block);

    override suspend fun shutdown() = pool.shutdown()
}