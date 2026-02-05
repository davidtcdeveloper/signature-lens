package com.signaturelens.core.encoding

import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

class HeicSupportChecker {
    fun isHeicSupported(): Boolean {
        // Bitmap.compress with HEIC format was added in API 30
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false
        }
        
        // Check if there is an encoder for HEIC or HEVC
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            
            for (type in info.supportedTypes) {
                if (type.equals(MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC, ignoreCase = true) ||
                    type.equals("image/heic", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
