package me.ztiany.androidav.camera.camera1;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.ztiany.lib.avbase.utils.Directory;
import me.ztiany.lib.avbase.utils.av.YUVUtils;
import timber.log.Timber;

/**
 * Camera1 API，后置摄像头【测试代码，只支持后置摄像头垂直拍摄】。
 */
public class Camera1SurfaceView extends SurfaceView {

    private Camera.Size size;

    private Camera mCamera;

    private volatile boolean isCapture;

    public Camera1SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                startPreview();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = mCamera.getParameters();
        size = parameters.getPreviewSize();
        Timber.d("size: width = %d, height = %d", size.width, size.height);

        try {
            mCamera.setPreviewDisplay(getHolder());
            mCamera.setDisplayOrientation(90);
            byte[] buffer = new byte[size.width * size.height * 3 / 2];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer((bytes, camera) -> {
                Timber.d("bytes == buffer: %b", buffer == bytes);//true
                if (isCapture) {
                    isCapture = false;
                    final byte[] output = new byte[bytes.length];
                    YUVUtils.nv21RotateCW(bytes, output, size.width, size.height, 90);
                    capture(output);
                }
                mCamera.addCallbackBuffer(bytes);
            });
            mCamera.startPreview();
        } catch (IOException ioException) {
            Timber.e(ioException, "startPreview");
        }
    }

    public void startCapture() {
        isCapture = true;
    }

    public void capture(byte[] temp) {
        //保存一张照片
        File pictureFile = Directory.createSDCardRootAppTimeNamingPath(Directory.PICTURE_FORMAT_JPEG);
        if (!pictureFile.exists()) {
            try {
                boolean succeeded = pictureFile.createNewFile();
                Timber.d("create %s. result: %b", pictureFile.getAbsolutePath(), succeeded);
                FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                //将 NV21 data 保存成 YuvImage
                YuvImage image = new YuvImage(
                        temp,
                        ImageFormat.NV21,
                        size.height,
                        size.width,
                        null
                );
                //图像压缩：将 NV21 格式图片，压缩成 Jpeg，并得到 JPEG 数据流。
                image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, fileOutputStream);
            } catch (IOException ioException) {
                Timber.e(ioException, "capture");
            }
        }
    }

}