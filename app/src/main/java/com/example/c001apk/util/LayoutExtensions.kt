package com.example.c001apk.util

import android.content.res.Resources
import android.text.StaticLayout

/**
 * Calculated the widest line in a [StaticLayout].
 */
internal fun StaticLayout.textWidth(): Int {
    var width = 0f
    for (i in 0 until lineCount) {
        width = width.coerceAtLeast(getLineWidth(i))
    }
    return width.toInt()
}

/**
 * Linearly interpolate between two values.
 */
internal fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

val Number.dp get() = (toFloat() * Resources.getSystem().displayMetrics.density).toInt()
