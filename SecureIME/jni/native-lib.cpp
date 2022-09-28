#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <unistd.h>
#include "jni.h"

#include <gui/Surface.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <android_runtime/android_view_Surface.h>
#include <android/log.h>

#include <aidl/nxp/hardware/secureime/ISecureIME.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>

using namespace android;
using aidl::nxp::hardware::secureime::ISecureIME;

static sp<Surface> surface;
static std::shared_ptr<ISecureIME> secureime_(nullptr);

ANativeWindowBuffer *nativeBuffer = nullptr;
ANativeWindow* nativeWindow = nullptr;

static jint
connectAIDL(JNIEnv *env, jobject thiz) {
    int ret = 0;

    auto secureimeName =  std::string() + ISecureIME::descriptor + "/default";
    ALOGI("connecting to AIDL %s ...\n", secureimeName.c_str());

    if (AServiceManager_isDeclared(secureimeName.c_str())) {
        ALOGI("waiting for AIDL service %s ...\n", secureimeName.c_str());
        auto secureimeBinder = ndk::SpAIBinder(AServiceManager_waitForService(secureimeName.c_str()));
        secureime_ = ISecureIME::fromBinder(secureimeBinder);
        if (!secureime_) {
            ALOGE("Failed to connect to AIDL %s!\n", secureimeName.c_str());
            ret = -1;
        }
    } else {
        ALOGE("AIDL %s is not declared!\n", secureimeName.c_str());
        ret = -1;
    }

    return ret;
}

static jint
startKeyboard(JNIEnv *env, jobject thiz, jobject jsurface){
    status_t err = NO_ERROR;
    int width, height;
    int bufCount = 0;
    int minUndequeuedBuf = 0;
    int fenceFd = -1;
    uint8_t* buffer = nullptr;
    int buffer_size;
    int BytesPerPixel, BytesPerStride;

    surface = android_view_Surface_getSurface(env, jsurface);
    if(!android::Surface::isValid(surface)){
        ALOGE("surface is invalid ");
        return EXIT_FAILURE;
    }
    nativeWindow = surface.get();

    err = native_window_api_connect(nativeWindow, NATIVE_WINDOW_API_CPU);
    if (err != NO_ERROR) {
        ALOGE("ERROR: fail in native_window_api_connect\n");
        return EXIT_FAILURE;
    }

    /* get width and height */
    err = nativeWindow->query(nativeWindow,
            NATIVE_WINDOW_WIDTH, &width);
    if (err != NO_ERROR) {
        ALOGE("error: NATIVE_WINDOW_WIDTH query "
                "failed: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }

    err = nativeWindow->query(nativeWindow,
            NATIVE_WINDOW_HEIGHT, &height);
    if (err != NO_ERROR) {
        ALOGE("error: NATIVE_WINDOW_HEIGHT query "
                "failed: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }

    /* set format */
    err = native_window_set_buffers_format(nativeWindow, PIXEL_FORMAT_RGBX_8888);
    if (err != NO_ERROR) {
        ALOGE("failed to set native_window_set_buffers_format!\n");
        return EXIT_FAILURE;
    }

    /* set usage */
    err = native_window_set_usage(nativeWindow, GRALLOC_USAGE_PROTECTED | GRALLOC_USAGE_PRIVATE_2);
    if (err != NO_ERROR) {
        ALOGE("failed to set native_window_set_buffers_format!\n");
        return EXIT_FAILURE;
    }

    /* set scale mode */
    err = native_window_set_scaling_mode(nativeWindow, NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW);
    if (err != NO_ERROR) {
        ALOGE("set scaling mode failed!");
        return EXIT_FAILURE;
    }

    /* allow allocating new buffer */
    static_cast<Surface*>(nativeWindow)->getIGraphicBufferProducer()->allowAllocation(true);

    /*  set buffer count */
    err = nativeWindow->query(nativeWindow, NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS, &minUndequeuedBuf);
    if (err != NO_ERROR) {
        ALOGE("query minUndequeuedBuf failed: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }
    bufCount = minUndequeuedBuf + 1;
    err = native_window_set_buffer_count(nativeWindow, bufCount);
    if (err != NO_ERROR) {
        ALOGE("failed to set buffer count!: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }

    err = nativeWindow->dequeueBuffer(nativeWindow, &nativeBuffer, &fenceFd);
    if (err != NO_ERROR) {
        ALOGE("failed to dequene buffer!: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }

    /* wait fence */
    sp<Fence> fence(new Fence(fenceFd));
    err = fence->waitForever("dequeueBuffer_EmptyNative");
    if (err != NO_ERROR) {
        ALOGE("failed to wait fence: %d", err);
        return EXIT_FAILURE;
    }

    /* lock buffer before use */
    sp<GraphicBuffer> buf(GraphicBuffer::from(nativeBuffer));
    err = buf->lock(GRALLOC_USAGE_SW_WRITE_OFTEN, (void**)(&buffer), &BytesPerPixel, &BytesPerStride);
    if (err != NO_ERROR) {
        ALOGE("error: lock failed: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }

    buffer_size = BytesPerStride * height;
    if (!secureime_) {
        ALOGE("secureime AIDL service is not initialized!");
    } else {
        int dup_fd = dup(nativeBuffer->handle->data[0]);
        auto status = secureime_->SecureIMEInit(::ndk::ScopedFileDescriptor(dup_fd), buffer_size, buf->getStride(), width, height, &err);
        if ((status.getExceptionCode() != EX_NONE) || (err < 0)) {
            ALOGE("AIDL call SecureIMEInit failed!");
            return EXIT_FAILURE;
        }
    }

    /* unlock buffer */
    err = buf->unlock();
    if (err != NO_ERROR) {
        ALOGE("error: unlock failed: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }

    /* queue the buffer */
    err = nativeWindow->queueBuffer(nativeWindow, buf->getNativeBuffer(), -1);
    if (err != NO_ERROR) {
        ALOGE("error: queueBuffer failed: %s (%d)", strerror(-err), -err);
        return EXIT_FAILURE;
    }

    return NO_ERROR;
}

static jint
deliverInput(JNIEnv *env, jobject thiz, jint x, jint y){
    int key = 0;

    if (!secureime_) {
        ALOGE("secureime AIDL service is not initialized!");
    } else {
        auto status = secureime_->SecureIMEHandleTouch(x, y, &key);
        if (status.getExceptionCode() != EX_NONE) {
            key = -1;
            ALOGE("AIDL call SecureIMEHandleTouch failed!");
        }
    }
    ALOGI("native get coordinate x %d, y %d key %d", x, y, key);

    return key;
}

static jint cleanup(JNIEnv *env, jobject thiz) {
    status_t err = NO_ERROR;

    /* cancel buffer */
    if (nativeBuffer != nullptr) {
        nativeWindow->cancelBuffer(nativeWindow, nativeBuffer, -1);
        nativeBuffer = nullptr;
    }

    /* clean up after success or error */
    if (nativeWindow != nullptr) {
        err = native_window_api_disconnect(nativeWindow, NATIVE_WINDOW_API_CPU);
        if (err != NO_ERROR) {
            ALOGE("error: api_disconnect failed: %s (%d)", strerror(-err), -err);
        }
        nativeWindow = nullptr;
    }

    /* close TA connection */
    if (!secureime_) {
        ALOGE("secureime AIDL service is not initialized!");
    } else {
        auto status = secureime_->SecureIMEExit(&err);
        if ((status.getExceptionCode() != EX_NONE) || (err < 0)) {
            ALOGE("AIDL call SecureIMEExit failed!");
        }
    }

    return err;
}

static JNINativeMethod sMethods[] = {
        {"connectAIDL",
         "()I",
         (void*)connectAIDL},
        {"startKeyboard",
         "(Landroid/view/Surface;)I",
         (void*)startKeyboard},
        {"deliverInput",
         "(II)I",
         (void*)deliverInput},
        {"cleanup",
         "()I",
         (void*)cleanup},
};

int registerNativeMethods(JNIEnv* env, const char* className,
                          JNINativeMethod* gMethods, int numMethods) {
    jclass clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    if (!registerNativeMethods(env, "com/android/nxpime/SecureIMESurfaceView",
                               sMethods, sizeof(sMethods) / sizeof(sMethods[0]))) {
        return -1;
    }
    return JNI_VERSION_1_4;
}
