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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.RemoteViews;

public class MyService extends Service {

	private MyReceiver receiver;
	private NotificationManager notificationManager;
	private int sleeptime;
	private int awaketime;


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub	
		return null;
	}

	@Override
	public void onCreate() {
		receiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();  
		filter.addAction("android.intent.action.MY_SLEEP_AWAKE");
		filter.addAction("android.intent.action.CANCEL_MY_SLEEP_AWAKE");
		registerReceiver(receiver, filter); 
	}

	@Override  
	public void onStart(Intent intent, int startId) {  
		//Get sleeptime and awaketime from the EditView
		sleeptime = intent.getIntExtra("sleeptime", 5);
		awaketime = intent.getIntExtra("awaketime", 5);

		//Set the notification
		notificationManager = (NotificationManager)this.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.lock, "sleepawake", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);
		notification.contentView = contentView;
		Intent cancelIntent = new Intent("android.intent.action.CANCEL_MY_SLEEP_AWAKE");
		PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, 0);
		notification.contentView.setOnClickPendingIntent(R.id.cancelButton, cancelPendingIntent);
		notificationManager.notify(0, notification);

		Intent newIntent = new Intent("android.intent.action.MY_SLEEP_AWAKE");
		newIntent.putExtra("action","sleep");
		newIntent.putExtra("SleepDelayTime",sleeptime);
		newIntent.putExtra("AwakeDelayTime",awaketime);
		sendBroadcast(newIntent);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
		notificationManager.cancel(0);
	}
}
