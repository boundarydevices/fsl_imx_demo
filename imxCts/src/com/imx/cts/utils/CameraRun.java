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

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.KeyEvent;

public class CameraRun extends AppRun {

    private static final String TAG = "CameraRun";
    public CameraRun(Context context) {
        super(context);
    }

    @Override
    public void run() {
        startUp();
    }

    private void startUp(){
        StringBuilder path = new StringBuilder();
        path.append(Environment.getExternalStorageDirectory().getPath()).append("/123.jpg");
        File file=new File(path.toString());
        Uri uri=FileProvider.getUriForFile(this.mContext, "com.imx.cts.fileprovider", file);
        Intent itent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        itent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        ((Activity)mContext).startActivityForResult(itent, 1);
    }

}
