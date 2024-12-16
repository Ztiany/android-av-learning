package me.ztiany.androidav.opengl.oglcamera

import android.hardware.Camera
import android.hardware.camera2.CameraDevice

interface ICamera

class Camera2(val cameraDevice: CameraDevice) : ICamera

class Camera1(val camera: Camera) : ICamera
