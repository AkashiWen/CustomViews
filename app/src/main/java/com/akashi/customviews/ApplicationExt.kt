package com.akashi.customviews

import android.annotation.SuppressLint
import android.app.Application


private var mApplicationContext: Application? = null

@SuppressLint("PrivateApi")
fun getContext(): Application? {
    if (mApplicationContext != null) return mApplicationContext
    try {
        val aClass = Class.forName("android.app.ActivityThread")
        aClass.getMethod("currentApplication").also {
            it.isAccessible = true
        }.let {
            mApplicationContext = it.invoke(null) as? Application
        }
    } catch (e: Throwable) {
    }
    return mApplicationContext
}