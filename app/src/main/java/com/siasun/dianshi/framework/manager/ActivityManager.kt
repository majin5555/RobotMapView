package com.siasun.dianshi.framework.manager

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import java.lang.ref.WeakReference

/**
 * Activity管理类
 */
object ActivityManager {

    private val tasks = mutableListOf<WeakReference<Activity>>()


    fun push(activity: Activity) {
          cleanUp()
        tasks.add(WeakReference(activity))
    }

    fun pop(activity: Activity) {
        val it = tasks.iterator()
        while (it.hasNext()) {
            val ref = it.next()
            val item = ref.get()
            if (item == null || item == activity) {
                it.remove()
            }
        }
    }

    fun top(): Activity? {
        cleanUp()
        return tasks.lastOrNull()?.get()
    }

    fun finishAllActivity(callback: (() -> Unit)? = null) {
        val it = tasks.iterator()
        while (it.hasNext()) {
            val item = it.next().get()
            it.remove()
            item?.finish()
        }
        callback?.invoke()
    }


    /**
     * 关闭其他activity
     */
     fun finishOtherActivity(clazz: Class<out Activity>) {
        val it = tasks.iterator()
        while (it.hasNext()) {
            val item = it.next().get()
            if (item == null) {
                it.remove()
            } else if (item::class.java != clazz) {
                it.remove()
                item.finish()
            }
        }
    }
    /**
     * 关闭activity
     */

    fun finishActivity(clazz: Class<out Activity>) {
        val it = tasks.iterator()
        while (it.hasNext()) {
            val item = it.next().get()
            if (item == null) {
                it.remove()
            } else if (item::class.java == clazz) {
                it.remove()
                item.finish()
                break
            }
        }
    }

    /**
     * activity是否在栈中
     */
      fun isActivityExistsInStack(clazz: Class<out Activity>?): Boolean {
        if (clazz != null) {
            for (ref in tasks) {
                val task = ref.get() ?: continue
                if (task::class.java == clazz) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Activity是否销毁
     * @param context
     */
    private fun findActivity(context: Context): Activity? {
        if (context is Activity) {
            return context
        } else if (context is ContextWrapper) {
            return findActivity(context.baseContext)
        }
        return null
    }


    /**
     * ContextWrapper是context的包装类，AppcompatActivity，service，application实际上都是ContextWrapper的子类
     * AppcompatXXX类的context都会被包装成TintContextWrapper
     * @param context
     */
      private fun cleanUp() {
        val it = tasks.iterator()
        while (it.hasNext()) {
            if (it.next().get() == null) {
                it.remove()
            }
        }
    }
}