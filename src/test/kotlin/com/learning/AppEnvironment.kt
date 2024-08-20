package com.learning

class AppEnvironment {
    val recorder = FakeRecorderHttp()
    val client = app(recorder)
}
