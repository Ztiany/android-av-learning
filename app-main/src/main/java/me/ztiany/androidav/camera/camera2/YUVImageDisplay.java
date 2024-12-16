package me.ztiany.androidav.camera.camera2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Size;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

class YUVImageDisplay {

     static void showYUVImage(
             Activity activity,
             byte[] nv21,
             int stride,
             Size previewSize,
             String openedCameraId,
             int displayOrientation,
             boolean isMirrorPreview,
             ImageView ivOriginFrame,
             ImageView ivPreviewFrame
     ) {
         byte[] jpgBytes = getARGBBytes(nv21, stride, previewSize);
         showImages(activity, openedCameraId, displayOrientation, isMirrorPreview, ivOriginFrame, ivPreviewFrame, jpgBytes);
     }

    @NotNull
    private static byte[] getARGBBytes(byte[] nv21, int stride, Size previewSize) {
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, stride, previewSize.getHeight(), null);
        // ByteArrayOutputStream的close中其实没做任何操作，可不执行。
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        //下面三种处理方式，任选一种。

        // 由于某些stride和previewWidth差距大的分辨率，[0,previewWidth)是有数据的，而[previewWidth,stride)补上的U、V均为0，因此在这种情况下运行会看到明显的绿边
        //  yuvImage.compressToJpeg(new Rect(0, 0, stride, previewSize.getHeight()), 100, byteArrayOutputStream);

        // 由于U和V一般都有缺损，因此若使用方式，可能会有个宽度为1像素的绿边
        yuvImage.compressToJpeg(new Rect(0, 0, previewSize.getWidth(), previewSize.getHeight()), 100, byteArrayOutputStream);

        // 为了删除绿边，抛弃一行像素
        //   yuvImage.compressToJpeg(new Rect(0, 0, previewSize.getWidth() - 1, previewSize.getHeight()), 100, byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private static void showImages(Activity activity, String openedCameraId, int displayOrientation, boolean isMirrorPreview, ImageView ivOriginFrame, ImageView ivPreviewFrame, byte[] jpgBytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        // 原始预览数据生成的bitmap
        final Bitmap originalBitmap = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.length, options);

        //预览相对于原数据需要进行修正【因为摄像头的方向问题】
        Matrix matrix = new Matrix();
        // 预览相对于原数据可能有旋转
        matrix.postRotate(Camera2Helper.CAMERA_ID_BACK.equals(openedCameraId) ? displayOrientation : -displayOrientation);

        // 对于前置数据，镜像处理；若手动设置镜像预览，则镜像处理；若都有，则不需要镜像处理
        if (Camera2Helper.CAMERA_ID_FRONT.equals(openedCameraId) ^ isMirrorPreview) {
            matrix.postScale(-1, 1);
        }
        // 和预览画面相同的bitmap
        final Bitmap previewBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);

        activity.runOnUiThread(() -> {
            ivOriginFrame.setImageBitmap(originalBitmap);
            ivPreviewFrame.setImageBitmap(previewBitmap);
        });
    }

}