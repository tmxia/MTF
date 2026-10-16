package com.aazhou.mtforum.data.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable







enum class LogLevel(val label: String) {
    INFO("Info"),
    WARN("Warn"),
    ERROR("Error"),
    ;

    
    fun admits(entry: LogLevel): Boolean = entry.ordinal >= ordinal
}







@Serializable
data class LogEntry(
    val id: Long,
    val time: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val detail: String? = null,
)

private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)


fun LogEntry.toText(): String = buildString {
    append('[').append(timeFormat.format(Date(time))).append("] ")
    append('[').append(level.name).append("] ")
    append('[').append(tag).append("] ")
    append(message)
    detail?.takeIf { it.isNotBlank() }?.let {
        append('\n').append(it)
    }
}






fun List<LogEntry>.toExportText(versionName: String, level: LogLevel): String = buildString {
    append("MTF v").append(versionName).append(" 日志导出\n")
    append("记录等级：").append(level.label)
        .append("（仅记录该等级及更严重的日志），共 ").append(size).append(" 条\n")
    append("--------------------------------------------------\n")
    this@toExportText.sortedBy { it.time }.forEach { entry ->
        append(entry.toText()).append('\n')
    }
}
