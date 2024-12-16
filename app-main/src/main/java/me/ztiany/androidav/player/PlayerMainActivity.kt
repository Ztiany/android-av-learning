package me.ztiany.androidav.player

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.androidav.databinding.PlayerActivityMainBinding
import me.ztiany.androidav.player.mediacodec.MediaCodecPlayerActivity
import me.ztiany.androidav.player.mediaplayer.VideoMediaPlayerActivity
import me.ztiany.androidav.player.openglplayer.OpenGNESPlayerActivity

class PlayerMainActivity : AppCompatActivity() {

    private lateinit var binding: PlayerActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlayerActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
    }

    fun setUpView() {
        //Media Player 视频渲染
        binding.playerByMediaPlayer.setOnClickListener {
            startActivity(Intent(this, VideoMediaPlayerActivity::class.java))
        }

        //MediaCodec + SurfaceView 视频渲染
        binding.playerByMedicCodec.setOnClickListener {
            startActivity(Intent(this, MediaCodecPlayerActivity::class.java))
        }

        //视频渲染
        binding.playerByMedicCodecEgl.setOnClickListener {
            startActivity(Intent(this, OpenGNESPlayerActivity::class.java))
        }
    }

}