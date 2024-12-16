#ifndef ANDROID_AV_SAFEQUEUECPP11_H
#define ANDROID_AV_SAFEQUEUECPP11_H

#include <queue>
#include <thread>
#include <pthread.h>

using namespace std;

template<typename T>
class SafeQueueCPP11 {

    typedef void (*ReleaseCallback)(T &);

    typedef void (*SyncHandle)(queue<T> &);

public:
    SafeQueueCPP11() {
    }

    ~SafeQueueCPP11() {
    }

    void push(T new_value) {
        //锁 和智能指针原理类似，自动释放
        lock_guard<mutex> lk(mt);
        if (work) {
            q.push(new_value);
            cv.notify_one();
        } else {
            if (releaseCallback) {
                releaseCallback(new_value)
            }
        }
    }

    int pop(T &value) {
        int ret = 0;
        //占用空间相对lock_guard 更大一点且相对更慢一点，但是配合条件必须使用它，更灵活
        unique_lock<mutex> lk(mt);
        //第二个参数 lambda表达式：false则不阻塞 往下走
        cv.wait(lk, [this] { return !work || !q.empty(); });
        if (!q.empty()) {
            value = q.front();
            q.pop();
            ret = 1;
        }
        return ret;
    }

    void setWork(int work) {
        lock_guard<mutex> lk(mt);
        this->work = work;
    }

    int empty() {
        return q.empty();
    }

    int size() {
        return q.size();
    }

    void clear() {
        lock_guard<mutex> lk(mt);
        int size = q.size();
        for (int i = 0; i < size; ++i) {
            T value = q.front();
            releaseHandle(value);
            q.pop();
        }
    }

    void sync() {
        lock_guard<mutex> lk(mt);
        syncHandle(q);
    }

    void setReleaseCallback(ReleaseCallback r) {
        releaseCallback = r;
    }

    void setSyncHandle(SyncHandle s) {
        syncHandle = s;
    }

private:
    mutex mt;
    condition_variable cv;
    queue<T> q;

    /**
     * 是否工作的标记
     *  1 ：工作
     *  0：不接受数据，不工作
     */
    int work;

    ReleaseCallback releaseCallback;
    SyncHandle syncHandle;
};

#endif //ANDROID_AV_SAFEQUEUECPP11_H