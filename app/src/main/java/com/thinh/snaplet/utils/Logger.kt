package com.thinh.snaplet.utils

import timber.log.Timber

object Logger {

    private const val LOG_TAG = "TIMBER"

    /** Log verbose message */
    fun v(message: String, vararg args: Any?) {
        Timber.tag(LOG_TAG).v(message, *args)
    }

    /** Log debug message */
    fun d(message: String, vararg args: Any?) {
        Timber.tag(LOG_TAG).d(message, *args)
    }

    /** Log info message */
    fun i(message: String, vararg args: Any?) {
        Timber.tag(LOG_TAG).i(message, *args)
    }

    /** Log warning message */
    fun w(message: String, vararg args: Any?) {
        Timber.tag(LOG_TAG).w(message, *args)
    }

    /** Log warning with throwable */
    fun w(throwable: Throwable, message: String = "", vararg args: Any?) {
        if (message.isEmpty()) {
            Timber.tag(LOG_TAG).w(throwable)
        } else {
            Timber.tag(LOG_TAG).w(throwable, message, *args)
        }
    }

    /** Log error message */
    fun e(message: String, vararg args: Any?) {
        Timber.tag(LOG_TAG).e(message, *args)
    }

    /** Log error with throwable */
    fun e(throwable: Throwable, message: String = "", vararg args: Any?) {
        if (message.isEmpty()) {
            Timber.tag(LOG_TAG).e(throwable)
        } else {
            Timber.tag(LOG_TAG).e(throwable, message, *args)
        }
    }

    /** Log WTF (What a Terrible Failure) - for critical errors */
    fun wtf(message: String, vararg args: Any?) {
        Timber.tag(LOG_TAG).wtf(message, *args)
    }

    /** Log WTF with throwable */
    fun wtf(throwable: Throwable, message: String = "", vararg args: Any?) {
        if (message.isEmpty()) {
            Timber.tag(LOG_TAG).wtf(throwable)
        } else {
            Timber.tag(LOG_TAG).wtf(throwable, message, *args)
        }
    }

    /** Plant a custom tree (for custom logging behavior) */
    fun plant(tree: Timber.Tree) {
        Timber.plant(tree)
    }

    /** Uproot a tree */
    fun uproot(tree: Timber.Tree) {
        Timber.uproot(tree)
    }

    /** Uproot all trees */
    fun uprootAll() {
        Timber.uprootAll()
    }
}