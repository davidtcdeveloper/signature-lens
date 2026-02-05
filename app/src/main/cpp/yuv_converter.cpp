#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>

#define TAG "SignatureLensNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)

inline int clamp(int v) {
    if (v < 0) return 0;
    if (v > 255) return 255;
    return v;
}

extern "C" JNIEXPORT void JNICALL
Java_com_signaturelens_core_native_NativeProcessor_convertYuvToRgb(
        JNIEnv* env,
        jobject /* this */,
        jobject yBuffer,
        jobject uBuffer,
        jobject vBuffer,
        jint width,
        jint height,
        jint yRowStride,
        jint uvRowStride,
        jint uvPixelStride,
        jobject outBuffer) {

    uint8_t* srcY = static_cast<uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    uint8_t* srcU = static_cast<uint8_t*>(env->GetDirectBufferAddress(uBuffer));
    uint8_t* srcV = static_cast<uint8_t*>(env->GetDirectBufferAddress(vBuffer));
    uint32_t* dst = static_cast<uint32_t*>(env->GetDirectBufferAddress(outBuffer));

    if (!srcY || !srcU || !srcV || !dst) {
        LOGE("Invalid buffers");
        return;
    }

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int yIdx = y * yRowStride + x;
            int uvX = x / 2;
            int uvY = y / 2;
            int uvIdx = uvY * uvRowStride + uvX * uvPixelStride;

            int Y = srcY[yIdx] & 0xFF;
            int U = (srcU[uvIdx] & 0xFF) - 128;
            int V = (srcV[uvIdx] & 0xFF) - 128;

            int R = clamp(Y + (int)(1.402f * V));
            int G = clamp(Y - (int)(0.344136f * U) - (int)(0.714136f * V));
            int B = clamp(Y + (int)(1.772f * U));

            dst[y * width + x] = (0xFF << 24) | (R << 16) | (G << 8) | B;
        }
    }
}
