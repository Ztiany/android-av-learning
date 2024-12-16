precision mediump float;

varying mediump vec2 vTextureCoordinate;
uniform sampler2D uTexture;

vec2 blurCoordinates[20];

uniform int width;
uniform int height;

void main(){
    vec2 singleStepOffset=vec2(1.0/float(width), 1.0/float(height));
    blurCoordinates[0] =vTextureCoordinate.xy+singleStepOffset* vec2(0.0, -10.0);
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
    vec4 currentColor=texture2D(uTexture, vTextureCoordinate);
    vec3 rgb=currentColor.rgb;
    for (int i = 0; i < 20; i++) {
        rgb+=texture2D(uTexture, blurCoordinates[i].xy).rgb;
    }
    vec4 blur = vec4(rgb*1.0/21.0, currentColor.a);
    vec4 highPassColor=currentColor-blur;
    highPassColor.r=clamp(2.0 * highPassColor.r * highPassColor.r * 24.0, 0.0, 1.0);
    highPassColor.g = clamp(2.0 * highPassColor.g * highPassColor.g * 24.0, 0.0, 1.0);
    highPassColor.b = clamp(2.0 * highPassColor.b * highPassColor.b * 24.0, 0.0, 1.0);
    vec4 highPassBlur=vec4(highPassColor.rgb, 1.0);
    float b =min(currentColor.b, blur.b);
    float value = clamp((b - 0.2) * 5.0, 0.0, 1.0);
    float maxChannelColor = max(max(highPassBlur.r, highPassBlur.g), highPassBlur.b);
    float intensity = 1.0;
    float currentIntensity = (1.0 - maxChannelColor / (maxChannelColor + 0.2)) * value * intensity;
    vec3 r =mix(currentColor.rgb, blur.rgb, currentIntensity);
    gl_FragColor=vec4(r, 1.0);
}