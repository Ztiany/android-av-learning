#include "log.h"
#include <vector>

void xLog(int priority, const char *tag, const char *format, ...) {
    // 定义日志最大长度
    const size_t MAX_LOG_LENGTH = 1000;

    // 初始化可变参数列表
    va_list args;
    va_start(args, format);

    /*
     * 计算格式化字符串所需的长度。
     *
     *  为什么要 copy 一次？在 C++ 中，可变参数列表是通过指针传递的，这意味着在遍历可变参数列表时会修改指针本身，而不仅仅是指针所指向的数据。
     *  因此，如果我们在函数中多次使用可变参数列表，可能会出现问题。为了避免这种问题，C++11 引入了 va_copy 函数，它可以创建可变参数列表的独
     *  立副本。这个副本与原始可变参数列表具有相同的状态，但是它们是相互独立的。这意味着我们可以在使用副本的同时保留原始可变参数列表的状态，
     *  而不会对它造成任何影响。
     */
    va_list args_copy;
    va_copy(args_copy, args);
    int formatted_length = vsnprintf(nullptr, 0, format, args_copy);
    va_end(args_copy);

    // 创建一个字符串缓冲区并将格式化字符串写入缓冲区
    std::vector<char> buffer(formatted_length + 1);
    vsnprintf(buffer.data(), buffer.size(), format, args);
    va_end(args);

    // 检查日志长度是否超过限制
    if (formatted_length <= MAX_LOG_LENGTH) {
        __android_log_print(priority, tag, "%s", buffer.data());
    } else {
        // 如果超过限制，分段输出
        const char *start = buffer.data();
        const char *end = start + formatted_length;
        while (start < end) {
            char chunk[MAX_LOG_LENGTH + 1];
            strncpy(chunk, start, MAX_LOG_LENGTH);
            chunk[MAX_LOG_LENGTH] = '\0';
            __android_log_print(priority, tag, "%s", chunk);
            start += MAX_LOG_LENGTH;
        }
    }
}

void xLogD(const char *tag, const char *format, ...) {
    va_list args;
    va_start(args, format);
    xLog(ANDROID_LOG_DEBUG, tag, format, args);
    va_end(args);
}

void xLogI(const char *tag, const char *format, ...) {
    va_list args;
    va_start(args, format);
    xLog(ANDROID_LOG_INFO, tag, format, args);
    va_end(args);
}

void xLogW(const char *tag, const char *format, ...) {
    va_list args;
    va_start(args, format);
    xLog(ANDROID_LOG_WARN, tag, format, args);
    va_end(args);
}

void xLogE(const char *tag, const char *format, ...) {
    va_list args;
    va_start(args, format);
    xLog(ANDROID_LOG_ERROR, tag, format, args);
    va_end(args);
}