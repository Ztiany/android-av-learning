package me.ztiany.androidav.camera.camera2;

import android.util.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.ztiany.lib.avbase.utils.Directory;
import me.ztiany.lib.avbase.utils.av.YUVUtils;

public class YUVSaver {

    private byte[] i420;
    private byte[] i420_rotated;

    private byte[] nv21;
    private byte[] nv21_rotated;

    private byte[] nv12;
    private byte[] nv12_rotated;

    //todo：适配横屏、前置摄像头
    public void saveYUV(byte[] y, byte[] u, byte[] v, Size previewSize, int stride, int displayOrientation, boolean isMirrorPreview) {
        int yuvLength = previewSize.getHeight() * previewSize.getWidth() * 3 / 2;
        if (i420 == null || i420.length != yuvLength) {
            i420 = new byte[yuvLength];
            i420_rotated = new byte[yuvLength];

            nv21 = new byte[yuvLength];
            nv21_rotated = new byte[yuvLength];

            nv12 = new byte[yuvLength];
            nv12_rotated = new byte[yuvLength];
        }

        YUVUtils.i420FromYUVCutToWidth(y, u, v, i420, stride, previewSize.getWidth(), previewSize.getHeight());
        YUVUtils.nv21FromYUVCutToWidth(y, u, v, nv21, stride, previewSize.getWidth(), previewSize.getHeight());
        YUVUtils.nv12FromYUVCutToWidth(y, u, v, nv12, stride, previewSize.getWidth(), previewSize.getHeight());

        YUVUtils.i420Rotate90CW(i420, i420_rotated, previewSize.getWidth(), previewSize.getHeight());
        YUVUtils.nv21RotateCW(nv21, nv21_rotated, previewSize.getWidth(), previewSize.getHeight(), 90);
        YUVUtils.nv12Rotate90CW(nv12, nv12_rotated, previewSize.getWidth(), previewSize.getHeight());

        save(i420, Directory.createSDCardRootAppTimeNamingPath("_" + previewSize.getWidth() + "x" + previewSize.getHeight() + "_i420" + Directory.VIDEO_FORMAT_YUV));
        save(nv21, Directory.createSDCardRootAppTimeNamingPath("_" + previewSize.getWidth() + "x" + previewSize.getHeight() + "_nv21" + Directory.VIDEO_FORMAT_YUV));
        save(nv12, Directory.createSDCardRootAppTimeNamingPath("_" + previewSize.getWidth() + "x" + previewSize.getHeight() + "_nv12" + Directory.VIDEO_FORMAT_YUV));
        save(i420_rotated, Directory.createSDCardRootAppTimeNamingPath("_" + previewSize.getHeight() + "x" + previewSize.getWidth() + "_i420" + Directory.VIDEO_FORMAT_YUV));
        save(nv21_rotated, Directory.createSDCardRootAppTimeNamingPath("_" + previewSize.getHeight() + "x" + previewSize.getWidth() + "_nv21" + Directory.VIDEO_FORMAT_YUV));
        save(nv12_rotated, Directory.createSDCardRootAppTimeNamingPath("_" + previewSize.getHeight() + "x" + previewSize.getWidth() + "_nv12" + Directory.VIDEO_FORMAT_YUV));
    }

    private void save(byte[] data, File file) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(data);
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
