package ru.spliterash.lettuceHelper.extensions

import java.util.regex.Pattern

private val REPLACE = Pattern.compile("[,.<>{}\\[\\]\"':;!@#$%^&*()\\-+=~]")

fun String.escapeRedis(): String {
    return REPLACE.matcher(this).replaceAll("\\\\$0")
}
