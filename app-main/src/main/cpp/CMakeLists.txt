cmake_minimum_required(VERSION 3.10.2)
project("androidav")

message("cmake running properties start-------------------------------------------------------")
message("PROJECT_NAME = " ${PROJECT_NAME})# 工程名称
message("PROJECT_SOURCE_DIR = " ${PROJECT_SOURCE_DIR}) #工程的根目录
message("CMAKE_SOURCE_DIR = " ${CMAKE_SOURCE_DIR})# 这是包含顶级 CMakeLists.txt 的目录，即顶级源目录。
message("CMAKE_CURRENT_SOURCE_DIR = " ${CMAKE_CURRENT_SOURCE_DIR}) #当前处理的 CMakeLists.txt 所在的路径
message("CMAKE_CURRENT_LIST_DIR = " ${CMAKE_CURRENT_LIST_DIR}) #CMakeLists.txt 的完整路径
message("PROJECT_BINARY_DIR = " ${PROJECT_BINARY_DIR}) #运行 cmake 命令的目录，通常是 ${PROJECT_SOURCE_DIR}/build
message("CMAKE_CURRENT_LIST_LINE = " ${CMAKE_CURRENT_LIST_LINE}) #当前所在的行
message("cmake running properties end-------------------------------------------------------")

#添加库头文件
include_directories(include/ffmpeg)
include_directories(include/openssl)
include_directories(include/fdk-aac)
include_directories(include/x2h64)
include_directories(include/lame)
include_directories(include/glm)

#添加本地头文件
include_directories(module/common)

#引入预编译库
set(JNI_LIBS ${CMAKE_SOURCE_DIR}/library/)
add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libavcodec.so)
add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libavfilter.so)
add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libavformat.so)
add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libavutil.so)
add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libswresample.so)
add_library(swscale SHARED IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libswscale.so)
add_library(ssl SHARED IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libssl.so)
add_library(crypto SHARED IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libcrypto.so)

#查找指定目录下的所有源文件，并将名称保存到指定的变量
file(
        GLOB SRC-FILES
        #common
        ${CMAKE_SOURCE_DIR}/module/*.cpp
        ${CMAKE_SOURCE_DIR}/module/common/*.cpp
        #opengl
        ${CMAKE_SOURCE_DIR}/module/opengl/*.cpp
        ${CMAKE_SOURCE_DIR}/module/opengl/egl/*.cpp
        ${CMAKE_SOURCE_DIR}/module/opengl/sample/*.cpp
        ${CMAKE_SOURCE_DIR}/module/opengl/common/*.cpp
        #opensl
        ${CMAKE_SOURCE_DIR}/module/opensl/*.cpp
)

#构建共享库
add_library(
        androidav
        SHARED
        ${SRC-FILES}
)

#链接
target_link_libraries(
        androidav
        # FFmpeg
        avcodec
        avfilter
        avformat
        avutil
        swresample
        swscale
        # openssl
        ssl
        crypto
        # android api
        log
        android
        GlESv2
        OpenSLES
)
