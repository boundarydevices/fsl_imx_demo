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

#include <util_alloc_buf.h>

static void msleep(uint64_t ms) {
    usleep(ms * 1000);
}

void rand_gen_param(Test_alloc_Window *win)
{
    unsigned int random_seed1,random_seed2,random_seed3,random_seed4;
    unsigned int random_fomat=0;
    unsigned int random_fomat_sel=0;
    unsigned int random_uage=0;
    unsigned int random_xsize=0;
    unsigned int random_ysize=0;
    unsigned int ignoral_val = GRALLOC_USAGE_HW_FB|GRALLOC_USAGE_HW_FBX|
        GRALLOC_USAGE_PRIVATE_0|GRALLOC_USAGE_PRIVATE_1|
        GRALLOC_USAGE_PRIVATE_2|GRALLOC_USAGE_PRIVATE_3|
        GRALLOC_USAGE_PRIVATE_MASK;
    random_seed1=rand();
    random_seed2=rand();
    random_seed3=rand();
    random_seed4=rand();
    random_fomat_sel=random_seed1%(sizeof(hal_format)/sizeof(hal_format[0]));
    random_fomat=hal_format[random_fomat_sel];
    random_uage=(random_seed2%32)&(~(ignoral_val));
    random_xsize=random_seed3%1920;
    random_ysize=random_seed4%1080;
    win->format	= random_fomat;
    win->usage	= random_uage;
    win->width	= random_xsize;
    win->height	= random_ysize;
    //INFO("format =%d usage=%d width=%d height=%d  \n",win->format, win->usage, win->width, win->height);
    //INFO("hal_format=%d  hal_fomat[0]=%d hal_format select=%d hal_format code=%d\n",
    //			sizeof(hal_format), sizeof(hal_format[0]),random_fomat_sel, random_fomat);
}

aBuffer *test_alloc_buf(Test_alloc_Window *win) {
    Test_alloc_Buffer *cnb;
    aBuffer *buf;
    int err;
    cnb = static_cast<Test_alloc_Buffer*>(malloc(sizeof(Test_alloc_Buffer)));
    if (!cnb)
        return 0;

    buf = &cnb->base;
    cnb->ffd = -1;

    buf->common.magic = ANDROID_NATIVE_BUFFER_MAGIC;
    buf->common.version = sizeof(aBuffer);
    buf->common.incRef = buf_inc_ref;
    buf->common.decRef = buf_dec_ref;

    err = win->gr->alloc(win->gr, win->width, win->height,
            win->format, win->usage, &buf->handle, &buf->stride);
    if (err) {
        ERROR("gralloc alloc buffer:  %d x %d fmt=0x%x usage=0x%x  failed: err=%d\n ",
                win->width, win->height, win->format, win->usage, err);
        free(buf);
        return 0;
    }
    printf("gralloc alloc buffer:  %d x %d fmt=0x%x usage=0x%x ",
            win->width, win->height, win->format, win->usage);

    return buf;
}


int test_gralloc_random(Test_alloc_Window *win) {
    hw_module_t const* module;
    alloc_device_t *gr;
    int err, i, n;
    int times = 0;

    if (hw_get_module(GRALLOC_HARDWARE_MODULE_ID, &module) != 0) {
        ERROR("cannot open gralloc module\n");
        return -ENODEV;
    }

    err = gralloc_open(module, &gr);
    if (err) {
        ERROR("couldn't open gralloc HAL (%s)", strerror(-err));
        return -ENODEV;
    }
    win->gr = gr;

    while (1) {
        rand_gen_param(win);
        aBuffer *buf = test_alloc_buf(win);
        INFO("alloc buf at %d times \n",times++);
        if (!buf)
        {
            ERROR("couldn't alloc more buffer\n");
            return -ENOMEM;
        }
        msleep(win->alloc_time);

    }
    return 0;
}

