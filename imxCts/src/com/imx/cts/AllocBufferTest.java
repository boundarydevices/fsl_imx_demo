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
package com.imx.cts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.test.InstrumentationTestCase;
import android.util.Log;
import com.imx.cts.utils.CameraRun;
import com.imx.AllocBufTestLaunchActivity;
import com.imx.AllocBufTestActivity;

public class AllocBufferTest extends InstrumentationTestCase {

    private Instrumentation mInstrumentation;
    private AllocBufTestActivity mActivity;
    private ExecutorService mThreadPool;
    private UiDevice mDevice;
    private boolean isDetectRun;
    private boolean isDetectPlay;
    private static final String TAG = "AllocBufferTest";
    private static final int MINUTE = 60;
    private static final int MID_WAIT = 5;
    private static final int LONG_WAIT = 15;
    private static final int SHORT_WAIT = 1;
    private static final String[] mWebSites = { "http://www.youku.com",
                                                "http://www.smashcat.org/av/canvas_test/",
                                                "http://www.sina.com.cn/",
                                                "https://www.google.com/",
                                                "http://stackoverflow.com/",
                                                "https://www.youtube.com/"};

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = launchActivity(mInstrumentation.getContext().getPackageName(),
                    AllocBufTestActivity.class, null);
        mThreadPool = Executors.newFixedThreadPool(5);
        mDevice = UiDevice.getInstance(getInstrumentation());
        mActivity.setUiDevice(mDevice);

        mDevice.pressHome();
        isDetectPlay = true;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mActivity.stopAllocRandomBuffer();
        mActivity.stopAppRun();
        mDevice.pressBack();
        sleep(SHORT_WAIT);
        mDevice.pressBack();
    }

    public void testStep01GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep01GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep01GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep02GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep02GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep02GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep03GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep03GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep03GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep04GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep04GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep04GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep05GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep05GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep05GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep06GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep06GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep06GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep07GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep07GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep07GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep08GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep08GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep08GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep09GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep09GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep09GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep10GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep10GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep10GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep11GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep11GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep11GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    public void testStep12GrallocBufferBrowser() throws UiObjectNotFoundException {
        browserTest();
    }

    public void testStep12GrallocBufferGallery() {
        AllocBufferWithGallery();
    }

    public void testStep12GrallocBufferCamera() {
        AllocBufferWithCamera();
    }

    private void browserTest() throws UiObjectNotFoundException {
        sleep(MID_WAIT);
        mActivity.startAllocRandomBuffer();
        runBrowser();
        assertActivtyCanLaunch();
    }

    private void AllocBufferWithGallery() {
        sleep(MID_WAIT);
        mActivity.startAllocRandomBuffer();
        mActivity.startGalleryRun();
        sleep(MID_WAIT);
        try {
            UiObject resumePlay = new UiObject(new UiSelector().text("Start over"));
            if (resumePlay.exists()) {
                resumePlay.click();
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        sleep(MINUTE * 3);
        assertActivtyCanLaunch();
    }

    private void AllocBufferWithCamera() {
        mActivity.startAllocRandomBuffer();
        testCameraOpen();
    }

    public void testCameraOpen() {
        try {
            mActivity.startCameraRun();
            sleep(MID_WAIT);
            UiObject allow = new UiObject(new UiSelector().resourceId("com.android.packageinstaller:id/permission_allow_button"));
            if (allow.exists()) {
                allow.click();
                sleep(MID_WAIT);
            }
            UiObject next = new UiObject(new UiSelector().resourceId("com.android.camera2:id/confirm_button"));
            if (next.exists()) {
                next.click();
                sleep(MID_WAIT);
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        sleep(LONG_WAIT);
        for (int i = 0; i < 10; i++) {
            Log.v(TAG, "test " + i + " times");
            UiObject shutter = new UiObject(new UiSelector().resourceId("com.android.camera2:id/shutter_button"));
            sleep(SHORT_WAIT);
            try {
                shutter.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
                fail("Camera cannot open successfully");
            }
            sleep(MID_WAIT);
            UiObject retake = new UiObject(new UiSelector().resourceId("com.android.camera2:id/retake_button"));
            try {
                retake.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
  //              fail("Camera cannot capture pic successfully");
            }
            sleep(MID_WAIT);
        }
        mDevice.pressBack();
    }

    private void assertActivtyCanLaunch() {
        mActivity.startTestActivity();
        sleep(MID_WAIT);
        assertTrue("activity cannot be started", mActivity.getStartActivityRes());
    }

    private void sleep(long time) {
        SystemClock.sleep(time * 1000);
    }

    private void runBrowser() throws UiObjectNotFoundException {
        for (String webSite : mWebSites) {
            mActivity.startBrowser(webSite);
            sleep(LONG_WAIT);
        }
    }

}
