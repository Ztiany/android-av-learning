#ifdef GL_ES
precision mediump float;
#endif
uniform vec4 aColor;
void main() {
    gl_FragColor = aColor;
}
