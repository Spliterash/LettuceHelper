package ru.spliterash.lettuceHelper.lock.exceptions

class UnlockSomeoneElseLockException : Exception("An attempt was made to unlock someone else's lock")