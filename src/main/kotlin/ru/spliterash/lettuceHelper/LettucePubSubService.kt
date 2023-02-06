package ru.spliterash.lettuceHelper

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.future.await
import ru.spliterash.lettuceHelper.base.pubsub.LettuceSubscribe
import java.util.concurrent.TimeUnit

class LettucePubSubService(private val client: RedisClient, private val uri: RedisURI) {
    suspend fun subscribe(channel: String, sub: (ByteArray) -> Unit): LettuceSubscribe {
        val connection = client.connectPubSubAsync(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec()), uri)
            .toCompletableFuture()
            .orTimeout(5, TimeUnit.SECONDS)
            .await()

        connection.addListener(object : RedisPubSubAdapter<String, ByteArray>() {
            override fun message(channel: String, message: ByteArray) {
                sub.invoke(message)
            }
        })
        connection.async().subscribe(channel).await()

        return object : LettuceSubscribe {
            override suspend fun unsubscribe() {
                connection.closeAsync().await()
            }
        }
    }
}