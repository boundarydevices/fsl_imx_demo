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
package com.imx.app;

import android.app.Activity;
import android.util.Log;
import android.view.WindowManager;
import android.os.Bundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

public class AllocBufferService extends Service {

    static {
        System.loadLibrary("imx_cts_app");
    }

    private static final String TAG = "AllocBufferService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mThreadPool = Executors.newFixedThreadPool(2);
        AllocBufferTask allocBufferTask = new AllocBufferTask();
        mThreadPool.execute(allocBufferTask);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(null);
        builder.setSmallIcon(R.drawable.lock);
        builder.setTicker("Foreground Service Start");
        builder.setContentTitle("Foreground Service");
        builder.setContentText("Make this service run in the foreground.");
        Notification notification = builder.build();

        startForeground(1, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private ExecutorService mThreadPool;

    class AllocBufferTask implements Runnable {

        @Override
        public void run() {
            long current = System.currentTimeMillis();
            long end = current + 120 * 1000;
            while (current < end){
                current = System.currentTimeMillis();
                nativeAllocBufferRandom();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                   e.printStackTrace();
                }
            }
        }
    }

    private native int nativeAllocBufferRandom();
}
