package me.ztiany.androidav.player.mediacodec

import android.media.MediaCodec
import android.media.MediaFormat
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

abstract class BaseDataDecoder(
    private val stateHolder: CodecPlayerStateHolder,
    private val mediaFormat: MediaFormat,
    private val provider: MediaDataProvider,
    private val renderer: MediaDataRenderer,
    private val syncToPts: Boolean = false,
    private val decodeTimeoutUs: Long = 10000L
) : MediaDataDecoder {

    private lateinit var decoder: MediaCodec

    private val isRunning = AtomicBoolean(false)
    private val inputDone = AtomicBoolean(false)
    private val outputDone = AtomicBoolean(false)

    private val inputLock by lazy { ReentrantLock() }
    private val inputCondition by lazy { inputLock.newCondition() }

    private val outputLock by lazy { ReentrantLock() }
    private val outputCondition by lazy { outputLock.newCondition() }

    private var startTimeForSync = -1L

    private val outputBufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    private val packInfo = PacketInfo(0, 0)

    override fun start() {
        if (isRunning.compareAndSet(false, true) && initDecoder()) {
            decoder.start()
            startInputWorker()
            startOutputWorker()
        }
    }

    private fun startInputWorker() {
        thread {
            startWork(inputLock, inputCondition, inputDone,
                task = {
                    pushData()
                },
                onPause = {

                },
                onWakeUp = {

                })
        }
    }

    private fun startOutputWorker() {
        thread {
            startWork(outputLock, outputCondition, outputDone,
                task = {
                    pullData()
                },
                onPause = {

                },
                onWakeUp = {
                    if (syncToPts) {
                        startTimeForSync = System.currentTimeMillis() - getCurrentPts()
                    }
                }
            )
        }
    }

    private fun initDecoder(): Boolean {
        return try {
            decoder = initDecoder(mediaFormat, renderer)
            true
        } catch (e: Exception) {
            false
        }
    }

    abstract fun initDecoder(mediaFormat: MediaFormat, renderer: MediaDataRenderer): MediaCodec

    override fun stop() {
        isRunning.set(false)
    }

    private fun startWork(
        lock: Lock,
        condition: Condition,
        workDone: AtomicBoolean,
        task: () -> Unit,
        onPause: () -> Unit,
        onWakeUp: () -> Unit,
    ) {
        outputDone.set(false)

        val onStateChanged: StateListener = {
            tryNotify(lock, condition)
        }
        stateHolder.addStateChanged(onStateChanged)

        while (!workDone.get()) {
            when {
                !isRunning.get() || stateHolder.isStopped -> {
                    workDone.set(true)
                    stop()
                    break
                }
                stateHolder.isPaused -> handleOnPause(lock, condition, onPause, onWakeUp)
                stateHolder.isStarted -> {
                    task()
                }
            }
        }

        stateHolder.removeStateChanged(onStateChanged)
        releaseDecoder()
    }

    private fun releaseDecoder() {
        if (inputDone.get() && outputDone.get()) {
            Timber.d("releaseDecoder")
            decoder.stop()
            decoder.release()
        }
    }

    private fun handleOnPause(
        lock: Lock,
        condition: Condition,
        onPause: () -> Unit,
        onWakeUp: () -> Unit
    ) {
        onPause()
        lock.lock()
        try {
            while (stateHolder.isPaused) {
                Timber.d("Decoder State: isPaused = true, do await")
                condition.await(5, TimeUnit.SECONDS)
            }
        } finally {
            lock.unlock()
        }
        onWakeUp()
    }

    private fun tryNotify(lock: Lock, condition: Condition) {
        lock.lock()
        try {
            if (!stateHolder.isPaused) {
                Timber.d("Decoder State: isPaused = false, do signal")
                condition.signal()
            }
        } finally {
            lock.unlock()
        }
    }

    private fun pushData() {
        if (inputDone.get()) {
            Timber.d("Decoder pushData reachEndOfStream")
            return
        }

        val inputBufferIndex = try {
            decoder.dequeueInputBuffer(decodeTimeoutUs)
        } catch (e: Exception) {
            Timber.e(e, "Decoder dequeueInputBuffer")
            -1
        }

        if (inputBufferIndex < 0) {
            //Timber.d("Decoder pushData inputBufferIndex < 0")
            return
        }

        val byteBuffer = decoder.getInputBuffer(inputBufferIndex) ?: return
        val readSize = provider.readPacket(byteBuffer, packInfo)

        if (readSize < 0) {
            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            inputDone.set(true)
            Timber.d("Decoder pushData reach EOF")
        } else {
            decoder.queueInputBuffer(inputBufferIndex, 0, readSize, packInfo.sampleTime, 0)
        }
    }

    private fun pullData() {
        val index = try {
            decoder.dequeueOutputBuffer(outputBufferInfo, decodeTimeoutUs)
        } catch (e: Exception) {
            Timber.e(e, "Decoder dequeueOutputBuffer")
            -1
        }

        when {
            index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                Timber.d("Decoder INFO_TRY_AGAIN_LATER")
                return
            }
            index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                Timber.d("Decoder INFO_OUTPUT_BUFFERS_CHANGED")
            }
            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                val outputFormat = decoder.outputFormat
                //It's not necessary to update format.
                //renderer.updateMediaFormat(outputFormat)
                Timber.d("Decoder INFO_OUTPUT_FORMAT_CHANGED with $outputFormat")
            }
            outputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 -> {
                Timber.d("Decoder pullData reach EOS")
                outputDone.set(true)
            }
            index >= 0 -> {
                doSync()

                decoder.getOutputBuffer(index)?.let {
                    deliverFrame(decoder, renderer, it, outputBufferInfo, index)
                }
            }
        }
    }

    private fun doSync() {
        if (!syncToPts) {
            return
        }

        if (startTimeForSync == -1L) {
            startTimeForSync = System.currentTimeMillis()
        }

        val passTime = System.currentTimeMillis() - startTimeForSync
        val currentPTS = getCurrentPts()
        if (currentPTS > passTime) {
            //Timber.d("doSync $currentPTS - $passTime = ${currentPTS - passTime}")
            Thread.sleep(currentPTS - passTime)
        }
    }

    abstract fun deliverFrame(decoder: MediaCodec, renderer: MediaDataRenderer, data: ByteBuffer, outputBufferInfo: MediaCodec.BufferInfo, index: Int)

    private fun getCurrentPts(): Long {
        return outputBufferInfo.presentationTimeUs / 1000
    }

}