//参考：https://github.com/wuhaoyu1990/MagicCamera/blob/master/Project-AndroidStudio/magicfilter/src/main/res/raw/beauty.glsl
precision lowp float;
uniform sampler2D uTexture;
varying  vec2 vTextureCoordinate;
uniform int width;
uniform int height;

const highp vec3 W = vec3(0.30, 0.59, 0.11);
vec2 blurCoordinates[20];

float hardLight(float color){
    if (color <= 0.5)
    color = color * color * 2.0;
    else
    color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);
    return color;
}

void main(){
    float params = 0.33;
    vec2 singleStepOffset = vec2(1.0/float(width), 1.0/float(height));
    vec3 centralColor = texture2D(uTexture, vTextureCoordinate).rgb;

    blurCoordinates[0] = vTextureCoordinate.xy + singleStepOffset * vec2(0.0, -10.0);
    blurCoordinates[1] = vTextureCoordinate.xy + singleStepOffset * vec2(0.0, 10.0);
    blurCoordinates[2] = vTextureCoordinate.xy + singleStepOffset * vec2(-10.0, 0.0);
    blurCoordinates[3] = vTextureCoordinate.xy + singleStepOffset * vec2(10.0, 0.0);
    blurCoordinates[4] = vTextureCoordinate.xy + singleStepOffset * vec2(5.0, -8.0);
    blurCoordinates[5] = vTextureCoordinate.xy + singleStepOffset * vec2(5.0, 8.0);
    blurCoordinates[6] = vTextureCoordinate.xy + singleStepOffset * vec2(-5.0, 8.0);
    blurCoordinates[7] = vTextureCoordinate.xy + singleStepOffset * vec2(-5.0, -8.0);
    blurCoordinates[8] = vTextureCoordinate.xy + singleStepOffset * vec2(8.0, -5.0);
    blurCoordinates[9] = vTextureCoordinate.xy + singleStepOffset * vec2(8.0, 5.0);
    blurCoordinates[10] = vTextureCoordinate.xy + singleStepOffset * vec2(-8.0, 5.0);
    blurCoordinates[11] = vTextureCoordinate.xy + singleStepOffset * vec2(-8.0, -5.0);
    blurCoordinates[12] = vTextureCoordinate.xy + singleStepOffset * vec2(0.0, -6.0);
    blurCoordinates[13] = vTextureCoordinate.xy + singleStepOffset * vec2(0.0, 6.0);
    blurCoordinates[14] = vTextureCoordinate.xy + singleStepOffset * vec2(6.0, 0.0);
    blurCoordinates[15] = vTextureCoordinate.xy + singleStepOffset * vec2(-6.0, 0.0);
    blurCoordinates[16] = vTextureCoordinate.xy + singleStepOffset * vec2(-4.0, -4.0);
    blurCoordinates[17] = vTextureCoordinate.xy + singleStepOffset * vec2(-4.0, 4.0);
    blurCoordinates[18] = vTextureCoordinate.xy + singleStepOffset * vec2(4.0, -4.0);
    blurCoordinates[19] = vTextureCoordinate.xy + singleStepOffset * vec2(4.0, 4.0);
    float sampleColor = centralColor.g * 20.0;

    sampleColor += texture2D(uTexture, blurCoordinates[0]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[1]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[2]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[3]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[4]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[5]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[6]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[7]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[8]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[9]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[10]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[11]).g;
    sampleColor += texture2D(uTexture, blurCoordinates[12]).g * 2.0;
    sampleColor += texture2D(uTexture, blurCoordinates[13]).g * 2.0;
    sampleColor += texture2D(uTexture, blurCoordinates[14]).g * 2.0;
    sampleColor += texture2D(uTexture, blurCoordinates[15]).g * 2.0;
    sampleColor += texture2D(uTexture, blurCoordinates[16]).g * 2.0;
    sampleColor += texture2D(uTexture, blurCoordinates[17]).g * 2.0;
    sampleColor += texture2D(uTexture, blurCoordinates[18]).g * 2.0;
    sampleColor += texture2D(uTexture, blurCoordinates[19]).g * 2.0;
    sampleColor = sampleColor / 48.0;

    float highPass = centralColor.g - sampleColor + 0.5;
    for (int i = 0; i < 5;i++){
        highPass = hardLight(highPass);
    }
    float luminance = dot(centralColor, W);
    float alpha = pow(luminance, params);
    vec3 smoothColor = centralColor + (centralColor-vec3(highPass))*alpha*0.1;
    gl_FragColor = vec4(mix(smoothColor.rgb, max(smoothColor, centralColor), alpha), 1.0);
}

