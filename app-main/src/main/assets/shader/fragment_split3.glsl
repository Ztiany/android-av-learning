precision mediump float;

varying vec2 vTextureCoordinate;
uniform sampler2D uTexture;

void main() {
    float y = vTextureCoordinate.y;
    float a = 1.0/3.0;

    if (y<a){
        y+=a;
    } else if (y>2.0*a){
        y -= 1.0/3.0;
    }

    gl_FragColor= texture2D(uTexture, vec2(vTextureCoordinate.x, y));
}