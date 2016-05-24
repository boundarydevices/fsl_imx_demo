/*
 * Copyright (C) 2016 Freescale Semiconductor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.imx.cts.utils;

import android.content.Context;
import android.util.Log;

public class GalleryRun extends AppRun {

    public GalleryRun(Context context) {
        super(context);
    }

    @Override
    public void run() {
        String path = "/sdcard/test/bbb_full/720x480/mp4_libx264_libfaac/bbb_full.ffmpeg.720x480.mp4.libx264_500kbps_25fps.libfaac_stereo_128kbps_44100Hz.mp4";
        String cmd = "am start -n com.android.gallery3d/com.android.gallery3d.app.MovieActivity " +
                " -d file://\\/" + path + " --user '0'";
        Log.v(TAG, cmd);
        CmdUtil.execCommand(cmd);
    }

}
