package me.ztiany.lib.avbase.camera.camera2;

import android.util.Size;

import androidx.annotation.NonNull;

import java.util.List;

public interface SizeSelector {

    @NonNull
    Size getBestSupportedSize(@NonNull List<Size> sizes);

}