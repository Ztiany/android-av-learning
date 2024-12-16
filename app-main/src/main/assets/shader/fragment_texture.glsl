#ifdef GL_ES
precision mediump float;
#endif
// 纹理
uniform sampler2D uTexture;
//纹理坐标
varying vec2 vTextureCoordinate;
void main() {
    vec4 color = texture2D(uTexture, vTextureCoordinate);
    gl_FragColor = color;
}
