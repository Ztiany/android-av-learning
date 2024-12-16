//使用 samplerExternalOES 需要添加下面标识
#extension GL_OES_EGL_image_external : require

#ifdef GL_ES
precision mediump float;
#endif

//纹理坐标
varying vec2 vTextureCoordinate;

//直接承载 YUV 的纹理
uniform samplerExternalOES uTexture;

void main(){
    vec4 rgba = texture2D(uTexture, vTextureCoordinate);
    gl_FragColor=vec4(rgba.r, rgba.g, rgba.b, rgba.a);
}