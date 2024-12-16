package me.ztiany.androidav.stream

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.androidav.databinding.StreamingActivityMainBinding
import me.ztiany.androidav.stream.livecamera.client.LiveCameraClientActivity
import me.ztiany.androidav.stream.livecamera.server.LiveCameraServerActivity
import me.ztiany.androidav.stream.livescreen.receiver.ScreenReceiverActivity
import me.ztiany.androidav.stream.livescreen.sender.ScreenSenderActivity

class StreamingMediaActivity : AppCompatActivity() {

    private lateinit var binding: StreamingActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StreamingActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
    }

    private fun setUpView() {
        //投屏
        binding.videoH265ProjectScreenReceiver.setOnClickListener {
            startActivity(Intent(this, ScreenReceiverActivity::class.java))
        }
        binding.videoH265ProjectScreenSender.setOnClickListener {
            startActivity(Intent(this, ScreenSenderActivity::class.java))
        }

        //视频相互传输
        binding.videoH265ShareCameraServer.setOnClickListener {
            startActivity(Intent(this, LiveCameraServerActivity::class.java))
        }
        binding.videoH265ShareCameraClient.setOnClickListener {
            startActivity(Intent(this, LiveCameraClientActivity::class.java))
        }
    }


}