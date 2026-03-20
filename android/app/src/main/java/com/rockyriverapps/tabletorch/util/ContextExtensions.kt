package com.rockyriverapps.tabletorch.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Finds the Activity from a Context, traversing ContextWrapper hierarchy if needed.
 * Returns null if no Activity is found (e.g., in Preview or Service contexts).
 *
 * @return The Activity if found, null otherwise
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
