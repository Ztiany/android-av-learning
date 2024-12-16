package me.ztiany.androidav.opengl.jwopengl.gles2

import android.opengl.Matrix
import timber.log.Timber

class GLMVPMatrix {

    companion object {
        /** Camera 在 Z 轴的位置 */
        private const val DEFAULT_CAMERA_Z = 5F

        /** 视点原点（Camera 位置）到近平面的距离 */
        private const val DEFAULT_NEAR = 1F

        /** 视点原点（Camera 位置）到远平面的距离 */
        private const val DEFAULT_FAR = 6F
    }

    /**用于接收 mvp 的计算结果*/
    val mvpMatrix = FloatArray(16)

    /**默认为单位矩阵*/
    val modelMatrix = floatArrayOf(
        1.0F, 0F, 0F, 0F,
        0.0F, 1.0F, 0F, 0F,
        0.0F, 0F, 1.0F, 0F,
        0.0F, 0F, 0F, 1.0F,
    )

    /**默认为单位矩阵*/
    val viewMatrix = floatArrayOf(
        1.0F, 0F, 0F, 0F,
        0.0F, 1.0F, 0F, 0F,
        0.0F, 0F, 1.0F, 0F,
        0.0F, 0F, 0F, 1.0F,
    )

    /** 默认为单位矩阵 */
    val projectionMatrix = floatArrayOf(
        1.0F, 0F, 0F, 0F,
        0.0F, 1.0F, 0F, 0F,
        0.0F, 0F, 1.0F, 0F,
        0.0F, 0F, 0F, 1.0F,
    )

    private var worldWidth = 0
    private var worldHeight = 0

    private var modelWidth = 0
    private var modelHeight = 0

    fun setWorldSize(width: Int, height: Int) {
        worldWidth = width
        worldHeight = height
    }

    fun setModelSize(width: Int, height: Int) {
        modelWidth = width
        modelHeight = height
    }

    fun getWorldWidth() = worldWidth
    fun getWorldHeight() = worldHeight
    fun getModelWidth() = modelWidth
    fun getModelHeight() = modelHeight

    /**一般的相机摆放位置*/
    fun lookAtNormally() {
        lookAt(
            0F, 0F, DEFAULT_CAMERA_Z,
            //看向原点
            0F, 0F, 0F,
            //人眼水平视角
            0F, 1F, 0F
        )
    }

    /**摆放相机*/
    fun lookAt(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float
    ) {
        Matrix.setLookAtM(
            viewMatrix, 0,
            //相机位置
            eyeX, eyeY, eyeZ,
            //看向哪个点【这个点要在裁剪区域内，即保证其在近平面与远平面的区间内】
            centerX, centerY, centerZ,
            //决定哪个坐标轴竖直向上，且该向量与视线是垂直的，可理解为人正常平视物体时，头顶所指方向为竖直向上向量，视线此刻与该向量垂直的。
            upX, upY, upZ
        )
    }

    /**设置为正交投影*/
    fun adjustToOrthogonal(
        near: Float = DEFAULT_NEAR,
        far: Float = DEFAULT_FAR
    ) {

        if (worldHeight == 0 || worldWidth == 0 || modelHeight == 0 || modelWidth == 0) {
            return
        }

        val worldRatio = worldWidth / worldHeight.toFloat()
        val originRatio = modelWidth / modelHeight.toFloat()

        Timber.d(
            "worldWidth = %d, worldHeight = %d, modelWidth = %d, modelHeight = %d, worldRatio = %f, originRatio = %f",
            worldWidth, worldHeight,
            modelWidth, modelHeight,
            worldRatio, originRatio
        )

        if (worldWidth > worldHeight) {
            if (originRatio > worldRatio) {
                val actualRatio = originRatio / worldRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -1F, 1F,
                    -actualRatio, actualRatio,
                    near, far
                )
            } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                val actualRatio = worldRatio / originRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -actualRatio, actualRatio,
                    -1F, 1F,
                    near, far
                )
            }
        } else {
            if (originRatio > worldRatio) {
                val actualRatio = originRatio / worldRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -1F, 1F,
                    -actualRatio, actualRatio,
                    near, far
                )
            } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                val actualRatio = worldRatio / originRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -actualRatio, actualRatio,
                    -1F, 1F,
                    near, far
                )
            }
        }
    }

    fun combineMVP() {
        val temp = floatArrayOf(
            0.0F, 0F, 0F, 0F,
            0.0F, 0.0F, 0F, 0F,
            0.0F, 0F, 0.0F, 0F,
            0.0F, 0F, 0F, 0.0F,
        )
        Matrix.multiplyMM(temp, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, temp, 0)
    }

    fun resetToIdentity(floatArray: FloatArray) {
        Matrix.setIdentityM(floatArray, 0)
    }

}