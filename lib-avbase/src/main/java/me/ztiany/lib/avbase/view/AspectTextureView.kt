package me.ztiany.lib.avbase.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import timber.log.Timber
import kotlin.math.abs

/**
 * 自动适配比例的布局
 */
class AspectTextureView : TextureView {

    private var mTargetAspect = -1.0

    /**
     * 是否自动适配尺寸
     */
    private var mIsAutoFit = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun setAspectRatio(aspectRatio: Double) {
        if (aspectRatio < 0) {
            throw IllegalArgumentException()
        }
        Timber.w("Setting aspect ratio to $aspectRatio (was $mTargetAspect)")
        if (mTargetAspect != aspectRatio) {
            mTargetAspect = aspectRatio
            requestLayout()
        }
    }

    fun setAutoFit(autoFit: Boolean) {
        mIsAutoFit = autoFit
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasure = widthMeasureSpec
        var heightMeasure = heightMeasureSpec
        if (!mIsAutoFit) {
            super.onMeasure(widthMeasure, heightMeasure)
            return
        }

        if (mTargetAspect > 0) {
            var initialWidth = MeasureSpec.getSize(widthMeasure)
            var initialHeight = MeasureSpec.getSize(heightMeasure)

            val horizontalPadding = paddingLeft + paddingRight
            val verticalPadding = paddingTop + paddingBottom
            initialWidth -= horizontalPadding
            initialHeight -= verticalPadding

            val viewAspectRatio = initialWidth.toDouble() / initialHeight
            val aspectDiff = mTargetAspect / viewAspectRatio - 1

            if (abs(aspectDiff) < 0.01) {
                Timber.w("aspect ratio is good (target= $mTargetAspect, view= $initialWidth x $initialHeight")
            } else {
                if (aspectDiff > 0) {
                    initialHeight = (initialWidth / mTargetAspect).toInt()
                } else {
                    initialWidth = (initialHeight * mTargetAspect).toInt()
                }
                initialWidth += horizontalPadding
                initialHeight += verticalPadding
                widthMeasure = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
                heightMeasure = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
            }
        }

        super.onMeasure(widthMeasure, heightMeasure)
    }

}
