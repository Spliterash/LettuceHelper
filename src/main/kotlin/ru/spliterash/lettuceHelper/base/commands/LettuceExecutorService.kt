package ru.spliterash.lettuceHelper.base.commands

import io.lettuce.core.api.StatefulConnection

interface LettuceExecutorService<T : StatefulConnection<*, *>> {
    suspend fun <R> execute(block: suspend T.() -> R): R

    suspend fun shutdown()
}