package com.taskpulse.app.worker

/**
 * Stable Long -> Int mapping for PendingIntent request codes.
 * Avoids raw toInt()/mod patterns that can collide unexpectedly.
 */
fun taskRequestCode(taskId: Long): Int {
    val mixed = taskId xor (taskId ushr 32)
    val positive = (mixed and 0x7FFF_FFFFL).toInt()
    return if (positive == 0) 1 else positive
}
