cmake_minimum_required(VERSION 3.10.2)
project("android_rtmp")

#添加头文件
include_directories(include)
include_directories(librtmp)

#添加 LibRTMP 子模块
add_subdirectory(librtmp)

#引入预编译库
set(JNI_LIBS ${CMAKE_SOURCE_DIR}/library/)
add_library(fdk_aac STATIC IMPORTED)
set_target_properties(fdk_aac PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libfdk-aac.a)
add_library(x264 STATIC IMPORTED)
set_target_properties(x264 PROPERTIES IMPORTED_LOCATION ${JNI_LIBS}/${ANDROID_ABI}/libx264.a)

#查找指定目录下的所有源文件，并将名称保存到指定的变量
aux_source_directory(module DIR_MODULE)
aux_source_directory(module/utils DIR_UTILS)
aux_source_directory(module/codec DIR_CODEC)
aux_source_directory(module/pusher DIR_PUSHER)

#构建共享库
add_library(
        ###库名
        android_rtmp
        ###共享库
        SHARED
        ###源码
        ${DIR_MODULE}
        ${DIR_UTILS}
        ${DIR_CODEC}
        ${DIR_PUSHER}
)

#找到依赖库
find_library(log-lib log)

#链接
target_link_libraries(
        android_rtmp
        rtmp
        x264
        fdk_aac
        log
)
