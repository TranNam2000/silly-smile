package com.jrm.service

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.jrm.utils.Logger
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared scheduler for ad reload (Banner, Native, etc.).
 * - Composite key by placeName + owner so same placeName in 2 Activities/Fragments has independent state.
 * - Owner lifecycle: onStop pause, onStart resume with remaining delay, onDestroy cancel.
 * - App lifecycle: background pause all, foreground resume only visible owners.
 */
object AdReloadScheduler {

    private val handler = Handler(Looper.getMainLooper())

    private val runnablesByKey = ConcurrentHashMap<String, Runnable>()
    private val activityRefsByKey = ConcurrentHashMap<String, WeakReference<Activity>>()
    private val lifecycleOwnerRefsByKey = ConcurrentHashMap<String, WeakReference<LifecycleOwner>>()
    private val lifecycleObserversByKey = ConcurrentHashMap<String, DefaultLifecycleObserver>()
    private val lastReloadTimeMsByKey = ConcurrentHashMap<String, Long>()
    private val intervalMsByKey = ConcurrentHashMap<String, Long>()
    private val placeLabelByKey = ConcurrentHashMap<String, String>()
    @Volatile
    private var appLifecycleObserverAdded = false

    /**
     * Composite key so same [placeName] in different Activities/Fragments has independent reload state.
     */
    fun getReloadKey(placeName: String, owner: LifecycleOwner): String =
        "${placeName}_${owner.javaClass.name}_${System.identityHashCode(owner)}"

    /**
     * Remaining ms until next reload is allowed. Returns 0 if reload can run now.
     */
    fun getRemainingReloadDelayMs(key: String, intervalMs: Long): Long {
        val lastMs = lastReloadTimeMsByKey[key] ?: 0L
        if (intervalMs <= 0L || lastMs == 0L) return 0L
        return (intervalMs - (System.currentTimeMillis() - lastMs)).coerceAtLeast(0L)
    }

    /**
     * Call when an ad was just shown for [key], so next schedule uses correct remaining delay.
     */
    fun markReloadDone(key: String) {
        lastReloadTimeMsByKey[key] = System.currentTimeMillis()
    }

    /**
     * Schedules a reload: run [onReload] after [initialDelayMs] (or [intervalMs] if null).
     * Observes [owner]: onStop pause, onStart resume with remaining delay, onDestroy cancel.
     * Observes app lifecycle: background pause all, foreground resume only visible owners.
     *
     * @param onReload Called when it's time to reload. Receives (Activity, LifecycleOwner?).
     *   Return false to reschedule with [intervalMs] (e.g. skip this run); return true when
     *   reload was triggered (caller will call [markReloadDone] and schedule again from load callback).
     */
    fun schedule(
        placeName: String,
        owner: LifecycleOwner,
        activity: Activity,
        intervalMs: Long,
        initialDelayMs: Long? = null,
        placeLabel: String = placeName,
        onReload: (Activity, LifecycleOwner?) -> Boolean
    ) {
        if (intervalMs <= 0L) return
        val key = getReloadKey(placeName, owner)
        intervalMsByKey[key] = intervalMs
        placeLabelByKey[key] = placeLabel
        ensureAppLifecycleObserver()
        removeScheduleOnly(key)

        val weakActivity = WeakReference(activity)
        lifecycleOwnerRefsByKey[key] = WeakReference(owner)

        val runnable = object : Runnable {
            override fun run() {
                handler.removeCallbacks(this)
                val act = weakActivity.get() ?: run { removeForKey(key); return }
                if (act.isFinishing || act.isDestroyed) {
                    removeForKey(key)
                    return
                }
                val lifecycleOwner = lifecycleOwnerRefsByKey[key]?.get() ?: (act as? LifecycleOwner)
                if (lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) != true) {
                    Logger.d("[$placeLabel] Reload paused (lifecycle not visible)")
                    return
                }
                val remainingMs = getRemainingReloadDelayMs(key, intervalMs)
                if (remainingMs > 0L) {
                    Logger.d("[$placeLabel] Chưa đủ interval, đợi thêm ${remainingMs}ms")
                    handler.postDelayed(this, remainingMs)
                    return
                }
                val didReload = onReload(act, lifecycleOwner)
                if (!didReload) {
                    handler.postDelayed(this, intervalMs)
                }
            }
        }

        runnablesByKey[key] = runnable
        activityRefsByKey[key] = weakActivity

        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                if (lifecycleOwnerRefsByKey[key]?.get() == owner) {
                    runnablesByKey[key]?.let { handler.removeCallbacks(it) }
                    Logger.d("[$placeLabel] Reload paused (lifecycle not visible)")
                }
            }

            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                if (lifecycleOwnerRefsByKey[key]?.get() == owner) {
                    postWithRemainingDelay(key, placeLabel)
                    Logger.d("[$placeLabel] Reload resumed (lifecycle visible)")
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                if (lifecycleOwnerRefsByKey[key]?.get() == owner) {
                    removeForKey(key)
                }
            }
        }
        lifecycleObserversByKey[key] = observer
        owner.lifecycle.addObserver(observer)

        val delayMs = initialDelayMs ?: intervalMs
        handler.postDelayed(runnable, delayMs)
        Logger.d(
            "[$placeLabel] Schedule reload ${intervalMs / 1000}s (owner: ${owner::class.java.simpleName})" +
                if (initialDelayMs != null) " (initial delay ${initialDelayMs}ms)" else ""
        )
    }

    /**
     * Cancels all reload state for [placeName] (all instances across Activities/Fragments).
     */
    fun cancelReload(placeName: String) {
        val prefix = placeName + "_"
        runnablesByKey.keys.filter { it.startsWith(prefix) }.toList().forEach { removeForKey(it) }
        Logger.d("[$placeName] Reload cancelled")
    }

    private fun removeScheduleOnly(key: String) {
        runnablesByKey.remove(key)?.let { handler.removeCallbacks(it) }
        val owner = lifecycleOwnerRefsByKey[key]?.get()
        lifecycleObserversByKey.remove(key)?.let { obs -> owner?.lifecycle?.removeObserver(obs) }
        lifecycleOwnerRefsByKey.remove(key)
        activityRefsByKey.remove(key)
        placeLabelByKey.remove(key)
    }

    private fun removeForKey(key: String) {
        removeScheduleOnly(key)
        lastReloadTimeMsByKey.remove(key)
        intervalMsByKey.remove(key)
    }

    private fun postWithRemainingDelay(key: String, placeLabel: String) {
        val runnable = runnablesByKey[key] ?: return
        handler.removeCallbacks(runnable)
        val intervalMs = intervalMsByKey[key] ?: 0L
        val remainingMs = getRemainingReloadDelayMs(key, intervalMs)
        if (remainingMs > 0L) {
            Logger.d("[$placeLabel] Resume reload: đợi thêm ${remainingMs}ms")
            handler.postDelayed(runnable, remainingMs)
        } else {
            handler.post(runnable)
        }
    }

    private fun ensureAppLifecycleObserver() {
        if (appLifecycleObserverAdded) return
        appLifecycleObserverAdded = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                runnablesByKey.values.forEach { handler.removeCallbacks(it) }
                Logger.d("Reload paused (app background)")
            }

            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                runnablesByKey.keys.toList().forEach { k ->
                    val lifecycleOwner = lifecycleOwnerRefsByKey[k]?.get()
                    val isVisible =
                        lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
                    if (isVisible) {
                        val label = placeLabelByKey[k] ?: k.substringBefore("_").ifEmpty { k }
                        postWithRemainingDelay(k, label)
                    }
                }
                Logger.d("Reload resumed (app foreground, visible only)")
            }
        })
    }
}
