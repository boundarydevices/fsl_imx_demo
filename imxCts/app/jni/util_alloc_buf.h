/*
 * Copyright (C) 2010-2016 Freescale Semiconductor, Inc. All Rights Reserved.
 */

/*
 * The code contained herein is licensed under the GNU General Public
 * License. You may obtain a copy of the GNU General Public License
 * Version 2 or later at the following locations:
 *
 * http://www.opensource.org/licenses/gpl-license.html
 * http://www.gnu.org/copyleft/gpl.html
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <strings.h>
#include <errno.h>
#include <unistd.h>
#include <pthread.h>
#include <fcntl.h>
#include <getopt.h>

#include <hardware/hardware.h>
#include <hardware/gralloc.h>
#include <hardware/hwcomposer.h>

#include <system/window.h>
#include <cutils/native_handle.h>

#include <cutils/log.h>
// normalize and shorten type names
typedef struct android_native_base_t aBase;
typedef struct ANativeWindowBuffer aBuffer;
typedef struct ANativeWindow aWindow;

#define LOG_TAG "ALLOC_BUFFER_TEST"


#define ERROR(...)     __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);
#define INFO(...)     __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);
#define LOG(...)     __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);



#define QCT_WORKAROUND 1

static int  hal_format[]= {
    HAL_PIXEL_FORMAT_RGBA_8888 ,
    HAL_PIXEL_FORMAT_RGBX_8888,
    HAL_PIXEL_FORMAT_RGB_565,
    HAL_PIXEL_FORMAT_BGRA_8888,
    HAL_PIXEL_FORMAT_YV12,
    HAL_PIXEL_FORMAT_YCbCr_420_888,
    HAL_PIXEL_FORMAT_YCrCb_420_SP,
    HAL_PIXEL_FORMAT_YCbCr_420_SP
};

static int  usage_list[]= {
    GRALLOC_USAGE_SW_READ_NEVER,
    GRALLOC_USAGE_SW_READ_RARELY,
    GRALLOC_USAGE_SW_READ_OFTEN,
    GRALLOC_USAGE_SW_READ_MASK,
    GRALLOC_USAGE_SW_WRITE_NEVER,
    GRALLOC_USAGE_SW_WRITE_RARELY,
    GRALLOC_USAGE_SW_WRITE_OFTEN,
    GRALLOC_USAGE_SW_WRITE_MASK,
    GRALLOC_USAGE_HW_TEXTURE,
    GRALLOC_USAGE_HW_RENDER,
    GRALLOC_USAGE_HW_2D,
    GRALLOC_USAGE_HW_COMPOSER,
    //    GRALLOC_USAGE_HW_FB,
    //    GRALLOC_USAGE_HW_FBX,
    GRALLOC_USAGE_EXTERNAL_DISP,
    GRALLOC_USAGE_PROTECTED,
    GRALLOC_USAGE_CURSOR,
    GRALLOC_USAGE_HW_VIDEO_ENCODER,
    GRALLOC_USAGE_HW_CAMERA_WRITE,
    GRALLOC_USAGE_HW_CAMERA_READ,
    GRALLOC_USAGE_HW_CAMERA_ZSL,
    GRALLOC_USAGE_HW_CAMERA_MASK,
    GRALLOC_USAGE_HW_MASK,
    GRALLOC_USAGE_RENDERSCRIPT,
    GRALLOC_USAGE_FOREIGN_BUFFERS,
    GRALLOC_USAGE_ALLOC_MASK,
    GRALLOC_USAGE_FORCE_CONTIGUOUS,
    //    GRALLOC_USAGE_PRIVATE_0,
    //    GRALLOC_USAGE_PRIVATE_1,
    //    GRALLOC_USAGE_PRIVATE_2,
    //    GRALLOC_USAGE_PRIVATE_3,
    //    GRALLOC_USAGE_PRIVATE_MASK
};


typedef struct Test_alloc_Buffer {
    aBuffer base;
    struct Test_alloc_Buffer *next;
    struct Test_alloc_Buffer *prev;
    int ffd;
} Test_alloc_Buffer;

typedef struct Test_alloc_Window {
    aWindow base;

    alloc_device_t *gr;

    Test_alloc_Buffer free_buffer_queue;

    unsigned width;
    unsigned height;
    unsigned xdpi;
    unsigned ydpi;
    unsigned format;
    unsigned usage;

    unsigned int alloc_time;
} Test_alloc_Window;


static void buf_inc_ref(aBase *base) {INFO ("buf %p ref++\n",base); }
static void buf_dec_ref(aBase *base) { INFO("buf %p ref--\n",base); }

static void msleep(uint64_t ms);
aBuffer *test_alloc_buf(Test_alloc_Window *win) ;
int test_gralloc_random(Test_alloc_Window *win) ;
