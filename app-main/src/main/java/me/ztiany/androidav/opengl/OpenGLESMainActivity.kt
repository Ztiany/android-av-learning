package me.ztiany.androidav.opengl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.ztiany.androidav.databinding.OpenglActivityMainBinding
import me.ztiany.androidav.opengl.jwopengl.JavaWithOpenGLMainActivity
import me.ztiany.androidav.opengl.nwopengl.NativeWithOpenGLMainActivity

class OpenGLESMainActivity : AppCompatActivity() {

    private lateinit var binding: OpenglActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OpenglActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openglBtnOpengles2Java.setOnClickListener {
            startActivity(Intent(this, JavaWithOpenGLMainActivity::class.java))
        }

        binding.openglBtnOpengles2Native.setOnClickListener {
            startActivity(Intent(this, NativeWithOpenGLMainActivity::class.java))
        }
    }

}