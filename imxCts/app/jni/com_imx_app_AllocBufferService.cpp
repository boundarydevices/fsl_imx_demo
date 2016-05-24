/*
 * Copyright (C) 2016 Freescale Semiconductor, Inc. All Rights Reserved.
 *
 * The code contained herein is licensed under the GNU General Public
 * License. You may obtain a copy of the GNU General Public License
 * Version 2 or later at the following locations:
 *
 * http://www.opensource.org/licenses/gpl-license.html
 * http://www.gnu.org/copyleft/gpl.html
 */
#include "jni.h"
#include "util_alloc_buf.h"

extern "C" {
    JNIEXPORT jint JNICALL Java_com_imx_app_AllocBufferService_nativeAllocBufferRandom(JNIEnv * env, jobject obj);
}

int test(int argc, char *argv[]);

JNIEXPORT jint JNICALL Java_com_imx_app_AllocBufferService_nativeAllocBufferRandom(JNIEnv * env, jobject obj){

    char* interval = "5";
    char* oom_adj_val = "0";
    int const param_size = 2;
    char* param[param_size] = {interval, oom_adj_val};
    test(param_size, param);
    return 0;
}
