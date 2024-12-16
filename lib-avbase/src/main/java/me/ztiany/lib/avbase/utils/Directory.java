package me.ztiany.lib.avbase.utils;

import android.annotation.SuppressLint;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

/**
 * @author Ztiany
 */
public class Directory {

    private static final String APP_NAME = "Alien-AV";

    private static final String TEMP_PICTURE = "temp-pictures";
    private static final String TEMP_FILES = "temp-files";

    public static final String PICTURE_FORMAT_JPEG = ".jpeg";
    public static final String PICTURE_FORMAT_PNG = ".png";
    public static final String VIDEO_FORMAT_MP4 = ".mp4";
    public static final String VIDEO_FORMAT_YUV = ".yuv";
    public static final String VIDEO_FORMAT_H264 = ".h264";
    public static final String VIDEO_FORMAT_H265 = ".h265";
    public static final String VIDEO_FORMAT_TXT = ".txt";
    public static final String AUDIO_FORMAT_PCM = ".pcm";
    public static final String AUDIO_FORMAT_WAV = ".wav";
    public static final String AUDIO_FORMAT_AAC = ".aac";

    ///////////////////////////////////////////////////////////////////////////
    // SDCard Root
    ///////////////////////////////////////////////////////////////////////////

    public static File createSDCardRootAppPath(String fileName) {
        File file = new File(getSDCardRootPath(), APP_NAME + "/" + fileName);
        makeParentPath(file, "createSDCardRootAppPath");
        return file;
    }

    public static File createSDCardRootAppTimeNamingPath(String format) {
        File file = new File(getSDCardRootPath(), APP_NAME + "/" + createTempFileName(format));
        makeParentPath(file, "createSDCardRootAppTimeNamingPath");
        return file;
    }

    /**
     * 获取 SD 卡根目录
     *
     * @return /storage/emulated/0/
     */
    private static File getSDCardRootPath() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return Environment.getExternalStorageDirectory();
        } else {
            return Utils.getApp().getFilesDir();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // DCIM
    ///////////////////////////////////////////////////////////////////////////

    @NonNull
    public static String createDCIMPath(String filename) {
        return createMediaPath(Environment.DIRECTORY_DCIM, filename);
    }

    @NonNull
    public static String createTimeNamingDCIMPath(String format) {
        return createTimeNamingMediaPath(Environment.DIRECTORY_DCIM, format);
    }

    @NonNull
    public static String createAudioPath(String filename) {
        return createMediaPath(Environment.DIRECTORY_MUSIC, filename);
    }

    @NonNull
    public static String createTimeNamingAudioPath(String format) {
        return createTimeNamingMediaPath(Environment.DIRECTORY_MUSIC, format);
    }

    /**
     * 获取外部存储路径。
     *
     * @return like /storage/sdcard0/APP_NAME/xxx.png
     */
    @NonNull
    private static String createTimeNamingMediaPath(String type, String format) {
        String path = getSDCardPublicMediaDirectoryPath(type).toString() + File.separator + APP_NAME + File.separator;
        File file = new File(path + createTempFileName(format));
        makeParentPath(file, "createTimeNamingMediaPath() called mkdirs: ");
        return file.getAbsolutePath();
    }

    /**
     * 获取外部媒体存储路径。
     *
     * @return like /storage/sdcard0/DCIM/APP_NAME/xxx.png
     */
    @NonNull
    private static String createMediaPath(String type, String filename) {
        String path = getSDCardPublicMediaDirectoryPath(type).toString() + File.separator + APP_NAME + File.separator;
        File file = new File(path + filename);
        makeParentPath(file, "createMediaPath() called mkdirs: ");
        return file.getAbsolutePath();
    }

    /**
     * 获取公共的外部存储目录。
     *
     * @param type {@link Environment#DIRECTORY_DOWNLOADS}, {@link Environment#DIRECTORY_DCIM}, ect
     * @return DIRECTORY_DCIM = /storage/sdcard0/DCIM , DIRECTORY_DOWNLOADS =  /storage/sdcard0/Download ...ect
     */
    private static File getSDCardPublicMediaDirectoryPath(@NonNull String type) {
        String state = Environment.getExternalStorageState();
        File dir;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            dir = Environment.getExternalStoragePublicDirectory(type);
        } else {
            dir = new File(Utils.getApp().getFilesDir(), type);
        }
        makePath(dir, "getSDCardPublicDirectoryPath");
        return dir;
    }

    ///////////////////////////////////////////////////////////////////////////
    // sd card private
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 根据日期生成一个临时的用于存储文件的全路径，格式由 format 制定。
     *
     * @return like /storage/emulated/0/Android/data/包名/cache/...
     */
    public static String createTempFilePath(String format) {
        String path = getAppExternalPrivateCachePath() + TEMP_FILES + File.separator + createTempFileName(format);
        makeParentPath(new File(path), "createTempFilePath() called mkdirs: ");
        return path;
    }

    /**
     * 根据日期生成一个临时的用于存储图片的全路径，格式由 format 制定。
     *
     * @return like /storage/emulated/0/Android/data/包名/cache/...
     */
    public static String createTempPicturePath(String format) {
        String path = getAppExternalPrivateCachePath() + TEMP_PICTURE + File.separator + createTempFileName(format);
        makeParentPath(new File(path), "createTempPicturePath() called mkdirs: ");
        return path;
    }

    /**
     * 获取 SD 卡上私有存储目录
     *
     * @return like /storage/emulated/0/Android/data/包名/PICTURE/...
     */
    private static String getAppExternalPrivatePath(String type) {
        String state = Environment.getExternalStorageState();
        String savePath;
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            savePath = Utils.getApp().getFilesDir() + File.separator;
        } else {
            savePath = Utils.getApp().getExternalFilesDir(type) + File.separator;
        }
        return savePath;
    }

    /**
     * 获取 SD 卡上私有缓存存储目录
     *
     * @return like /storage/emulated/0/Android/data/包名/cache/
     */
    private static String getAppExternalPrivateCachePath() {
        String state = Environment.getExternalStorageState();
        String savePath;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            savePath = Utils.getApp().getExternalCacheDir() + File.separator;
        } else {
            savePath = Utils.getApp().getCacheDir() + File.separator;
        }
        return savePath;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Tools
    ///////////////////////////////////////////////////////////////////////////

    private static void makeParentPath(File file, String tag) {
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                boolean mkdirs = parentFile.mkdirs();
                Timber.d("%s : %s", tag, mkdirs);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static void makePath(File dir, String tag) {
        try {
            if (dir != null && !dir.exists()) {
                boolean mkdirs = dir.mkdirs();
                Timber.d("%s : %s", tag, mkdirs);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private static final ThreadLocal<SimpleDateFormat> SDF_HOLDER = new ThreadLocal<SimpleDateFormat>() {

        @SuppressLint("ConstantLocale")
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        }

    };

    /**
     * 根据日期生成一个临时文件名，格式由 format 制定。
     */
    public static String createTempFileName(String format) {
        String tempFileName = Objects.requireNonNull(SDF_HOLDER.get()).format(new Date());
        return tempFileName + format;
    }

}