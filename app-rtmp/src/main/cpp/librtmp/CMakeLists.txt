# 关闭 ssl，LibRTMP 默认支持 SSL，这里没有则需要关闭。
# 如果使用 openssl，务必使用 openssl1.0.x 版本的，否则会有编译错误。具体参考 https://stackoverflow.com/questions/62273993/hmac-ctx-has-incomplete-type
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

# 收集源文件
file(GLOB rtmp_source *.c)

# 编译成 rtmp 静态库
add_library(
        ###库名
        rtmp
        ###静态库
        STATIC
        ###源码
        ${rtmp_source}
)