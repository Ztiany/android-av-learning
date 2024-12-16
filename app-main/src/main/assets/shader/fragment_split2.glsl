precision mediump float;

varying vec2 vTextureCoordinate;
uniform sampler2D uTexture;

void main() {
    float y = vTextureCoordinate.y;

    if (y<0.5){
        y+=0.25;
    } else {
        y -= 0.25;
    }

    gl_FragColor= texture2D(uTexture, vec2(vTextureCoordinate.x, y));
}