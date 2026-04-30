package com.taskpulse.app.worker

fun taskRequestCode(taskId: Long): Int {
    val bounded = if (taskId in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
        taskId.toInt()
    } else {
        (taskId % Int.MAX_VALUE).toInt()
    }
    return bounded.coerceAtLeast(1)
}
