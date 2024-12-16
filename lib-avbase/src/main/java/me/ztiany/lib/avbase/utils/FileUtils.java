package me.ztiany.lib.avbase.utils;

import android.content.res.AssetFileDescriptor;

import com.blankj.utilcode.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class FileUtils {

    public static void copyAssets(String assetsName, String path) throws IOException {
        AssetFileDescriptor assetFileDescriptor = Utils.getApp().getAssets().openFd(assetsName);
        FileChannel from = new FileInputStream(assetFileDescriptor.getFileDescriptor()).getChannel();
        FileChannel to = new FileOutputStream(path).getChannel();
        from.transferTo(assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength(), to);
    }

    public static String loadAssets(String assetsName) throws IOException {
        InputStream inputStream = Utils.getApp().getAssets().open(assetsName);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            byteArrayOutputStream.write(buffer, 0, read);
        }
        return byteArrayOutputStream.toString();
    }

    public static void writeBytes(OutputStream outputStream, byte[] array, boolean endFlag) {
        try {
            outputStream.write(array);
            if (endFlag) {
                outputStream.write('\n');
                outputStream.write('\n');
                outputStream.write('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static void writeContent(FileWriter writer, byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        try {
            writer.write(sb.toString());
            writer.write("\n");
            writer.write("\n");
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}