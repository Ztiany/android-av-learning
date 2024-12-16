#ifdef GL_ES
precision mediump float;
#endif

//纹理坐标
varying vec2 vTextureCoordinate;

//直接承载 YUV 的纹理
uniform sampler2D uTexture;

//缩放比例【由外部传入】
uniform  float scalePercent;
//放大的图与原图混合时的透明比例【由外部传入】
uniform  float mixPercent;

void main() {
    //取一个放大的中心点【这里是纹理中心】，然后以这个点为中心放大纹理
    float soulX = 0.5 + (vTextureCoordinate.x - 0.5) / scalePercent;
    float soulY = 0.5 + (vTextureCoordinate.y - 0.5) / scalePercent;

    //原图
    vec4 textureColor = texture2D(uTexture, vTextureCoordinate);
    //放大的图
    vec4 textureScaledColor= texture2D(uTexture, vec2(soulX, soulY));

    //混合原图和放大的图，就形成了灵魂出窍的效果
    //mix 是内置函数，其公式为：textureColor * (1.0 - alpha) + textureScaledColor * alpha
    gl_FragColor= mix(textureColor, textureScaledColor, mixPercent);
}