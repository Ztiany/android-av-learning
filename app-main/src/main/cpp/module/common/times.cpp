#include "times.h"
#include <sys/time.h>

//当前时间戳 clock
long long getNowInMillisecond() {
    struct timeval tv{};
    gettimeofday(&tv, nullptr);
    int sec = tv.tv_sec % 360000;
    long long t = sec * 1000 + tv.tv_usec / 1000;
    return t;
}

float getFrameTime() {
    static unsigned long long lastTime = 0, currentTime = 0;
    timeval current;
    gettimeofday(&current, nullptr);
    currentTime = current.tv_sec * 1000 + current.tv_usec / 1000;
    unsigned long long frameTime = lastTime == 0 ? 0 : currentTime - lastTime;
    lastTime = currentTime;
    return float(frameTime) / 1000.0f;
}