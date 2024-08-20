package com.learning

class AppEnvironment {
    private val recorder = FakeRecorderHttp()
    val client = app(recorder)
}
