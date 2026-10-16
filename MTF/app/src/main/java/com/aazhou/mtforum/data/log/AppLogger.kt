package com.aazhou.mtforum.data.log

import android.util.Log as AndroidLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull


typealias LogcatMirror = (LogLevel, String, String, Throwable?) -> Unit

private val logcatMirror: LogcatMirror = { level, tag, message, throwable ->
    when (level) {
        LogLevel.INFO -> AndroidLog.i(tag, message, throwable)
        LogLevel.WARN -> AndroidLog.w(tag, message, throwable)
        LogLevel.ERROR -> AndroidLog.e(tag, message, throwable)
    }
}







class AppLogger(
    private val store: LogStore,
    private val scope: CoroutineScope,
    private val mirror: LogcatMirror = logcatMirror,
) {

    private val _level = MutableStateFlow(LogLevel.INFO)

    
    val level: StateFlow<LogLevel> = _level.asStateFlow()

    fun setLevel(level: LogLevel) {
        _level.value = level
    }

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        flush: Boolean = false,
    ) {
        mirror(level, tag, message, throwable)
        if (!_level.value.admits(level)) return
        val detail = throwable?.stackTraceToString()
        if (flush) {
            runBlocking {
                withTimeoutOrNull(FLUSH_TIMEOUT_MS) { store.append(level, tag, message, detail) }
            }
        } else {
            scope.launch { store.append(level, tag, message, detail) }
        }
    }

    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    fun warn(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.WARN, tag, message, throwable)

    fun error(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, throwable)

    



    fun logCrash(thread: Thread, throwable: Throwable) {
        val type = throwable.javaClass.name
        log(
            level = LogLevel.ERROR,
            tag = TAG_CRASH,
            message = "未捕获异常（线程 ${thread.name}）：$type：${throwable.message}",
            throwable = throwable,
            flush = true,
        )
        runBlocking {
            withTimeoutOrNull(FLUSH_TIMEOUT_MS) { store.markCrashed() }
        }
    }

    companion object {
        
        const val TAG_CRASH = "Crash"

        
        private const val FLUSH_TIMEOUT_MS = 1000L
    }
}
