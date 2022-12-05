package ru.spliterash.lettuceHelper.extensions

import com.redis.lettucemod.api.StatefulRedisModulesConnection
import com.redis.lettucemod.api.async.RedisModulesAsyncCommands
import ru.spliterash.lettuceHelper.base.commands.LettuceExecutorService


suspend inline fun <T, R> LettuceExecutorService<StatefulRedisModulesConnection<String, T>>.executeAsync(
    crossinline block: suspend RedisModulesAsyncCommands<String, T>.() -> R
): R {
    return this.execute {
        block(this.async())
    }
}
