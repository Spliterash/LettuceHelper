# Lettuce Helper
Небольшая котлиновская обёртка над Lettuce

```kotlin
repositories {
    maven {
        url = uri("https://repo.spliterash.ru/group/")
    }
}

dependencies {
    api("ru.spliterash:lettuce-helper:1.0.0")
}
```

Позволяет легко выполнять запросы к redis, а так же содержит некоторые полезные велос... ой, то есть инструменты,
например распределённые блокировки