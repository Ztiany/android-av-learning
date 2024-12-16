package me.ztiany.androidav.camera.camera1;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import me.ztiany.androidav.R;

/**
 * get yuv data from back camera.
 */
public class Camera1Activity extends AppCompatActivity {

    private Camera1SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity_api1);
        mSurfaceView = findViewById(R.id.camera_surface);
    }

    public void capture(View view) {
        mSurfaceView.startCapture();
    }

}