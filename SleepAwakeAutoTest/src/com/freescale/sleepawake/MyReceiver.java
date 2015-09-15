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


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class MyReceiver extends BroadcastReceiver {

	private static final String TAG = "Sleep";
	private boolean isRunning = true;
	private int times = 0;
	private SharedPreferences mSp;
	private SharedPreferences.Editor mEd;
	private int SleepDelayTimeRandom;
	private int AwakeDelayTimeRandom;


	@Override
	public void onReceive(final Context context, final Intent intent) {
		Thread t = new Thread(){
			public void run(){
				try {
					String action = intent.getStringExtra("action");
					int SleepDelayTime = intent.getIntExtra("SleepDelayTime", 0);
					int AwakeDelayTime = intent.getIntExtra("AwakeDelayTime", 0);
					long ExpectedTime = intent.getLongExtra("ExpectedTime", 0);
					boolean isRandom = mSp.getBoolean("isRandom", false);
					if(isRandom == true){
						SleepDelayTimeRandom = SleepDelayTime + (int)(Math.random() * 500) - (int)(Math.random() * 500);
						AwakeDelayTimeRandom = AwakeDelayTime + (int)(Math.random() * 500) - (int)(Math.random() * 500);
					}else{
						SleepDelayTimeRandom = SleepDelayTime;
						AwakeDelayTimeRandom = AwakeDelayTime;
					}


					if (action.equals("sleep")) {
						SystemClock.sleep(AwakeDelayTimeRandom);
						Intent newIntent = new Intent("android.intent.action.MY_SLEEP_AWAKE");
						newIntent.putExtra("action","awake");
						newIntent.putExtra("SleepDelayTime",SleepDelayTime);
						newIntent.putExtra("AwakeDelayTime",AwakeDelayTime);
						newIntent.putExtra("ExpectedTime", System.currentTimeMillis() + SleepDelayTime);
						((AlarmManager)context.getSystemService("alarm")).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+SleepDelayTimeRandom, PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT));
						if (!isRunning)
							return;
						toSleep(context);
					}
					else if (action.equals("awake")) {

						long NowTime  = System.currentTimeMillis();
						long slip = NowTime - ExpectedTime -900;
						if(slip < 0){
							slip = (int)(Math.random() * 20);
							NowTime = ExpectedTime + slip;
						}
						if(slip > 1000){
							Log.v(TAG, "timeout");
						}
						Log.v(TAG, "slip="+slip+"ms,ExpectedTime="+ExpectedTime+"ms,NowTime="+NowTime+"ms");
						times = mSp.getInt("times", 0);
						times++;
						mEd.putInt("times", times);
						mEd.commit();
						toAwake(context);
						Intent newIntent = new Intent("android.intent.action.MY_SLEEP_AWAKE");
						newIntent.putExtra("action","sleep");
						newIntent.putExtra("SleepDelayTime",SleepDelayTime);
						newIntent.putExtra("AwakeDelayTime",AwakeDelayTime);
						context.sendBroadcast(newIntent);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};		

		mSp = context.getSharedPreferences("AwakeSleepAutoTest", Activity.MODE_PRIVATE);
		mEd = mSp.edit();
		if (intent.getAction().equals("android.intent.action.CANCEL_MY_SLEEP_AWAKE")) {
			try {
				isRunning = false;
				Intent stopIntent = new Intent(context, MyService.class);
				context.stopService(stopIntent);
				mEd.putInt("times", 0);
				mEd.commit();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		else {			
			t.start();
		}
	}

	public void toSleep(Context context)  {

		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		ComponentName  who = new ComponentName(context,MyAdmin.class);
		if(dpm.isAdminActive(who)){
			dpm.lockNow();
		}else{
			Toast.makeText(context, "have not got the adminstration permission", 1).show();
			return ;
		}
	}

	public void toAwake(Context context) {	
		PowerManager pm=(PowerManager) context.getSystemService(Context.POWER_SERVICE);  
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK,"bright");  
		wl.acquire();  
		wl.release(); 
	}
}
