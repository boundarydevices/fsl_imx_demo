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
package com.imx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;
import android.view.View;
import com.imx.cts.utils.AppRun;
import com.imx.cts.utils.GalleryRun;
import com.imx.cts.utils.CameraRun;
import android.net.Uri;

public class AllocBufTestActivity extends Activity {

    private Intent intent;
    private ExecutorService mThreadPool;
    private boolean isAppRun;
    private boolean hasAllocBuff;
    private UiDevice mDevice;
    private boolean isStartActivitySuccess;
    private static final String TAG = "TestActivity";

    public void setUiDevice(UiDevice device){
        this.mDevice = device;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThreadPool = Executors.newFixedThreadPool(5);
        isAppRun = true;
        hasAllocBuff = false;
    }

    public void click(View view){
        CameraRun cameraRun = new CameraRun(this);
        cameraRun.run();
    }

    public void startAllocRandomBuffer(){
        intent = new Intent();
        intent.setAction("com.imx.app.allocbuffer");
        intent.setPackage("com.imx.app");
        hasAllocBuff = true;
        startService(intent);
    }

    public void stopAllocRandomBuffer(){
        if (hasAllocBuff) {
            hasAllocBuff = false;
            stopService(intent);
        }
    }

    public void startGalleryRun(){
        mThreadPool.execute(new Runnable() {

            @Override
            public void run() {
                AppRun appRun = new GalleryRun(AllocBufTestActivity.this);
                while (isAppRun){
                    appRun.run();
                    SystemClock.sleep(1000 * 110);
                }
            }
        });
    }

    public void startCameraRun() {
        AppRun appRun = new CameraRun(AllocBufTestActivity.this);
        appRun.run();
    }

    public void startBrowser(String uri){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(uri));
        startActivity(i);
    }

    public void stopAppRun(){
        isAppRun = false;
    }

    public void startTestActivity(){
        Intent intent=new Intent();
        intent.setClass(this, AllocBufTestLaunchActivity.class);
        Bundle bundle=new Bundle();
        bundle.putString("str1", "test");
        intent.putExtras(bundle);
        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case 1:
                isStartActivitySuccess = data.getBooleanExtra("success", false);
                break;
            default:
                break;
        }
    }

    public boolean getStartActivityRes(){
        return isStartActivitySuccess;
    }


    private void sleep(long time){
        SystemClock.sleep(time * 1000);
    }
}

