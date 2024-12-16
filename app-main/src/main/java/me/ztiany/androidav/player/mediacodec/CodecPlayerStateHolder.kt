package me.ztiany.androidav.player.mediacodec

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

private const val NONE = 1
private const val PREPARED = 2
private const val STARTED = 3
private const val PAUSED = 4
private const val STOPPED = 5

typealias StateListener = (Int) -> Unit

class CodecPlayerStateHolder {

    private val onStateListeners = CopyOnWriteArrayList<StateListener>()

    private val currentState = AtomicInteger(NONE)

    fun addStateChanged(stateListener: StateListener) {
        onStateListeners.add(stateListener)
    }

    fun removeStateChanged(stateListener: StateListener) {
        onStateListeners.remove(stateListener)
    }

    private fun notifyNewState() {
        val newState = currentState.get()
        onStateListeners.forEach { it(newState) }
    }

    fun switchToPrepared(): Boolean {
        if (currentState.compareAndSet(NONE, PREPARED)) {
            notifyNewState()
            return true
        }
        return false
    }

    fun switchToStarted(): Boolean {
        if (currentState.compareAndSet(PREPARED, STARTED)) {
            notifyNewState()
            return true
        }
        if (currentState.compareAndSet(PAUSED, STARTED)) {
            notifyNewState()
            return true
        }
        return false
    }

    fun switchToNone(): Boolean {
        if (currentState.get() != NONE) {
            currentState.set(NONE)
            notifyNewState()
        }
        return true
    }

    fun switchToPause(): Boolean {
        if (currentState.compareAndSet(STARTED, PAUSED)) {
            notifyNewState()
            return true
        }
        return false
    }

    fun switchToStop(): Boolean {
        if (currentState.get() != STOPPED) {
            currentState.set(STOPPED)
            notifyNewState()
        }
        return true
    }

    val isStarted: Boolean
        get() = currentState.get() == STARTED

    val isPrepared: Boolean
        get() = currentState.get() == PREPARED

    val isPaused: Boolean
        get() = currentState.get() == PAUSED

    val isStopped: Boolean
        get() = currentState.get() == STOPPED

}