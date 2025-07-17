package me.ztiany.lib.avbase.camera.camera2;

import android.util.Size;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

public class DefaultSizeSelector implements SizeSelector {

    private final Size maxPreviewSize;

    private final Size minPreviewSize;

    private final Size previewViewSize;

    private final Size specifiedPreviewSize;

    private DefaultSizeSelector(Builder builder) {
        Timber.d("DefaultSizeSelector is constructed with: %s", builder);
        maxPreviewSize = builder.maxPreviewSize;
        minPreviewSize = builder.minPreviewSize;
        previewViewSize = builder.previewViewSize;
        specifiedPreviewSize = builder.previewSize;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @NonNull
    @Override
    public Size getBestSupportedSize(@NonNull List<Size> sizes) {
        Size defaultSize = sizes.get(0);
        sizes = sortSizes(sizes.toArray(new Size[0]));

        Timber.d("getBestSupportedSize: all sizes camera supports");
        for (Size size : sizes) {
            Timber.d("%d x %d", size.getWidth(), size.getHeight());
        }

        Timber.d("getBestSupportedSize: filter undesired");
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.getWidth()
                        || sizes.get(i).getHeight() > maxPreviewSize.getHeight()
                ) {
                    Size remove = sizes.remove(i);
                    Timber.d("remove %d x %d", remove.getWidth(), remove.getHeight());
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.getWidth()
                        || sizes.get(i).getHeight() < minPreviewSize.getHeight()
                ) {
                    Size remove = sizes.remove(i);
                    Timber.d("remove %d x %d", remove.getWidth(), remove.getHeight());
                }
            }
        }

        if (sizes.isEmpty()) {
            Timber.w("can not find suitable previewSize, now using default: %d x %d"
                    , defaultSize.getWidth(), defaultSize.getHeight());
            return defaultSize;
        }

        Size bestSize = sizes.get(0);

        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.getWidth() / (float) previewViewSize.getHeight();
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }
        Timber.w("getBestSupportedSize: previewViewRatio = 1 / previewViewRatio = %f", previewViewRatio);

        for (Size candidate : sizes) {
            if (specifiedPreviewSize != null
                    && specifiedPreviewSize.getWidth() == candidate.getWidth()
                    && specifiedPreviewSize.getHeight() == candidate.getHeight()
            ) {
                Timber.d("getBestSupportedSize: returning %d x %d", candidate.getWidth(), candidate.getHeight());
                return candidate;
            }
            /*
            get the minimal deviation size.
                best:
                   2160 / 1080 = 1.996...
                    1 / 1.996 = 0.502...

                option:
                    1920x1080
                        1080 / 1920 = 0.563...
                        0.563 - 0.502 =
             */
            if (Math.abs((candidate.getHeight() / (float) candidate.getWidth()) - previewViewRatio)
                    < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)
            ) {
                bestSize = candidate;
            }
        }

        Timber.d("getBestSupportedSize: returning %d x %d", bestSize.getWidth(), bestSize.getHeight());
        return bestSize;
    }

    @NonNull
    private static List<Size> sortSizes(Size[] tempSizes) {
        List<Size> sizes;

        // 降序
        //noinspection ComparatorMethodParameterNotUsed
        Arrays.sort(tempSizes, (o1, o2) -> {
            if (o1.getWidth() > o2.getWidth()) {
                return -1;
            } else if (o1.getWidth() == o2.getWidth()) {
                return o1.getHeight() > o2.getHeight() ? -1 : 1;
            } else {
                return 1;
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        return sizes;
    }

    public static class Builder {

        /**
         * 屏幕的长宽，在选择最佳相机比例时用到。
         */
        private Size previewViewSize;

        /**
         * 指定的预览宽高，若系统支持则会以这个预览宽高进行预览。
         */
        private Size previewSize;

        /**
         * 最大分辨率。
         */
        private Size maxPreviewSize;

        /**
         * 最小分辨率。
         */
        private Size minPreviewSize;

        private Builder() {
        }

        public Builder previewSize(Size size) {
            this.previewSize = size;
            return this;
        }

        public Builder maxPreviewSize(Size size) {
            this.maxPreviewSize = size;
            return this;
        }

        public Builder minPreviewSize(Size size) {
            this.minPreviewSize = size;
            return this;
        }

        public Builder previewViewSize(Size size) {
            this.previewViewSize = size;
            return this;
        }

        public SizeSelector build() {
            if (previewViewSize == null) {
                Timber.w("previewViewSize is null, now use default previewSize");
            }
            if (maxPreviewSize != null && minPreviewSize != null) {
                if (maxPreviewSize.getWidth() < minPreviewSize.getWidth() || maxPreviewSize.getHeight() < minPreviewSize.getHeight()) {
                    throw new IllegalArgumentException("maxPreviewSize must equal to or greater than minPreviewSize");
                }
            }
            return new DefaultSizeSelector(this);
        }

        @NonNull
        @Override
        public String toString() {
            return "Builder{" +
                    "previewViewSize=" + previewViewSize +
                    ", previewSize=" + previewSize +
                    ", maxPreviewSize=" + maxPreviewSize +
                    ", minPreviewSize=" + minPreviewSize +
                    '}';
        }
    }

}