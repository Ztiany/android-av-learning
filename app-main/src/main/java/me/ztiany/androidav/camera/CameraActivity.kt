package me.ztiany.androidav.camera

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.androidav.camera.camera1.Camera1Activity
import me.ztiany.androidav.camera.camera2.Camera2EncodingActivity
import me.ztiany.androidav.camera.camera2.Camera2PreviewActivity
import me.ztiany.androidav.databinding.CameraActivityMainBinding

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: CameraActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CameraActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun camera1(view: View) {
        startActivity(Intent(this, Camera1Activity::class.java))
    }

    fun camera2(view: View) {
        startActivity(Intent(this, Camera2PreviewActivity::class.java))
    }

    fun camera2Encoding(view: View) {
        startActivity(Intent(this, Camera2EncodingActivity::class.java))
    }

}