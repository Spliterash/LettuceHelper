package ru.spliterash.lettuceHelper.base.pubsub

interface LettucePubSubscribe {
    /**
     * Подписаться на событие
     */
    suspend fun subscribe(channel: String, sub: (ByteArray) -> Unit): LettuceSubscribe;
}