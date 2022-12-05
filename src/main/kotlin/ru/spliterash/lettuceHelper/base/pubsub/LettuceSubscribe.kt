package ru.spliterash.lettuceHelper.base.pubsub

@FunctionalInterface
interface LettuceSubscribe {
    suspend fun unsubscribe()
}