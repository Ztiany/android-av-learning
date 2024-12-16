package me.ztiany.androidav.avtest

import android.net.Uri
import android.os.Bundle
import com.android.sdk.mediaselector.common.ResultListener
import com.android.sdk.mediaselector.system.newSystemMediaSelector
import me.ztiany.androidav.databinding.ToolsActivityBinding
import me.ztiany.lib.avbase.app.activity.BaseActivity
import me.ztiany.lib.avbase.utils.av.printMediaCodecInfo
import timber.log.Timber

class AvTestActivity : BaseActivity<ToolsActivityBinding>() {

    private val systemMediaSelector by lazy {
        newSystemMediaSelector(this, object : ResultListener {
            override fun onTakeSuccess(result: List<Uri>) {
                Timber.d("onTakeSuccess: $result")
                result.getOrNull(0)?.let {
                    onMediaFileTaken?.invoke(it)
                }
            }
        })
    }

    private var testCase: TestCase? = null

    private var onMediaFileTaken: ((Uri) -> Unit)? = null

    override fun setUpLayout(savedInstanceState: Bundle?) {
        binding.btnCodecAbility.setOnClickListener {
            printCodecAbilities()
        }

        binding.btnMetadataRetrieve.setOnClickListener {

        }

        binding.btnMediaExtractorMt.setOnClickListener {
            onMediaFileTaken = {
                testCase = MediaExtractorMultiThreadingCase(this, it).apply { start() }
            }
            systemMediaSelector.takeFileFromSystem().mimeType("video/*").start()
        }

        binding.btnMediaExtractorAio.setOnClickListener {
            onMediaFileTaken = {
                testCase = MediaExtractorAllInOneCase(this, it).apply { start() }
            }
            systemMediaSelector.takeFileFromSystem().mimeType("video/*").start()
        }
    }

    private fun printCodecAbilities() {
        printMediaCodecInfo()
    }

    override fun onStop() {
        super.onStop()
        testCase?.stop()
    }

}