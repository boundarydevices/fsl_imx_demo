/*
 * Copyright (C) 2015 Freescale Semiconductor, Inc.
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
package com.freescale.sleepawake;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

	private static final String TAG = "MyReceiver";
	@SuppressWarnings("deprecation")
	private static KeyguardManager.KeyguardLock keyguardLock = null;
	private ExecutorService executorService = Executors.newSingleThreadExecutor();
	private boolean isRunning = true;
	private int counter = 0;
	private long SleepTimeStamp = 0;
	private long AwakeTimeStamp = 0;
	private long BeforewakeUp_TimeStamp = 0;
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals("android.intent.action.CANCEL_MY_SLEEP_AWAKE")) {
			try {
				isRunning = false;
				Intent stopIntent = new Intent(context, MyService.class);
				context.stopService(stopIntent);
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			executorService.submit(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						String action = intent.getStringExtra("action");
						int SleepDelayTime = intent.getIntExtra("SleepDelayTime", 0);
						int AwakeDelayTime = intent.getIntExtra("AwakeDelayTime", 0);
						
						if (action.equals("sleep")) {
							SystemClock.sleep(SleepDelayTime);
							if (!isRunning)
								return;
							toSleep(context, SleepDelayTime);
							
							Intent newIntent = new Intent("android.intent.action.MY_SLEEP_AWAKE");
							newIntent.putExtra("action","awake");
							newIntent.putExtra("SleepDelayTime",SleepDelayTime);
							newIntent.putExtra("AwakeDelayTime",AwakeDelayTime);
							((AlarmManager)context.getSystemService("alarm")).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+AwakeDelayTime, PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT));
						}
						else if (action.equals("awake")) {
							toAwake(context, AwakeDelayTime);
							
							Intent newIntent = new Intent("android.intent.action.MY_SLEEP_AWAKE");
							newIntent.putExtra("action","sleep");
							newIntent.putExtra("SleepDelayTime",SleepDelayTime);
							newIntent.putExtra("AwakeDelayTime",AwakeDelayTime);
							context.sendBroadcast(newIntent);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			});
		}
	}
		
	public void toSleep(Context context, int SleepDelayTime){
		SleepTimeStamp = System.currentTimeMillis();
		((PowerManager)context.getSystemService("power")).goToSleep(SystemClock.uptimeMillis());
		enableKeyGuardLock();
		Log.v(TAG, "sleep");
	}
	
	@SuppressLint("NewApi")
	public void toAwake(Context context, int AwakeDelayTime){
		((PowerManager)context.getSystemService("power")).wakeUp(SystemClock.uptimeMillis());
		AwakeTimeStamp = System.currentTimeMillis();
		SystemClock.sleep(500);
		disableKeyGuardLock(context);
	    Log.v(TAG, "awake");
	}
	
	
	@SuppressWarnings("deprecation")
	private void disableKeyGuardLock(Context context)
	{
		if (keyguardLock == null)
			keyguardLock = ((KeyguardManager)context.getSystemService("keyguard")).newKeyguardLock("MainReceiver");
		keyguardLock.disableKeyguard();
	}
	
	@SuppressWarnings("deprecation")
	private void enableKeyGuardLock()
	{
		if (keyguardLock != null)
			keyguardLock.reenableKeyguard();
	}
	
}
