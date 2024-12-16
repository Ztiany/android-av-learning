package me.ztiany.androidav.avapi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.androidav.avapi.audio.audiorecord.AudioRecordActivity
import me.ztiany.androidav.avapi.audio.autiotrack.AudioTrackActivity
import me.ztiany.androidav.avapi.audio.autiotrack.MediaExtractorAudioTrackActivity
import me.ztiany.androidav.avapi.audio.mediaplayer.AudioMediaPlayerActivity
import me.ztiany.androidav.avapi.audio.mediarecord.MediaRecordActivity
import me.ztiany.androidav.avapi.audio.mixing.MixingAudioActivity
import me.ztiany.androidav.avapi.audio.opensles.OpenSLESActivity
import me.ztiany.androidav.avapi.screen.ScreenRecordingActivity
import me.ztiany.androidav.avapi.video.combine.VideoCombinationActivity
import me.ztiany.androidav.databinding.ActivityMediaApiBinding

class MediaApiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaApiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaApiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpAudioViews()
        setUpVideoViews()
    }

    private fun setUpAudioViews() {
        //AudioRecord 录制 WAV
        binding.audioRecordByAr.setOnClickListener {
            startActivity(Intent(this, AudioRecordActivity::class.java))
        }

        //MediaRecord 录制 AAC
        binding.audioRecordByMr.setOnClickListener {
            startActivity(Intent(this, MediaRecordActivity::class.java))
        }

        //AudioTrack 播放 WAV
        binding.audioPlayByAt.setOnClickListener {
            startActivity(Intent(this, AudioTrackActivity::class.java))
        }

        //MediaPlayer 播放音频
        binding.audioPlayByMp.setOnClickListener {
            startActivity(Intent(this, AudioMediaPlayerActivity::class.java))
        }

        //MediaExtractor + AudioTrack 播放音频
        binding.audioPlayByMeAt.setOnClickListener {
            startActivity(Intent(this, MediaExtractorAudioTrackActivity::class.java))
        }

        //OpenSLES
        binding.audioOpensl.setOnClickListener {
            startActivity(Intent(this, OpenSLESActivity::class.java))
        }

        //混音
        binding.audioMixingAudio.setOnClickListener {
            startActivity(Intent(this, MixingAudioActivity::class.java))
        }
    }

    private fun setUpVideoViews() {
        //录频
        binding.videoScreenRecorder.setOnClickListener {
            startActivity(Intent(this, ScreenRecordingActivity::class.java))
        }

        //视频拼接
        binding.videoMosaic.setOnClickListener {
            startActivity(Intent(this, VideoCombinationActivity::class.java))
        }
    }

}