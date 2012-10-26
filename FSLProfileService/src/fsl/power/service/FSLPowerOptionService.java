/*
 * Copyright (C) 2012 Freescale Semiconductor, Inc.
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




package fsl.power.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import fsl.power.service.PowerServiceDB.Profiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class FSLPowerOptionService extends Service{

	private static final String TAG = "FSL_POWER_SERVICE";
	private static final boolean DEBUG = false;
	private static final int RDSIZE = 100;

	private NotificationManager mNM;
	private int NOTIFICATION = R.string.start;

	private Timer timer;

	// date and time
	private int mYear;

	private int mMonth;

	private int mDay;

	private int mHour;

	private int mMinute;

   private int scheduletime = 1;
   // use thermal to adjust the cpufreq
   private boolean gNothermal = false;
   static private int gTempKeeper = 50;
   static private int gMaxcpus = 1;
   static private int gSocType = 0;
   static private int gSocRev = 0;
   static private long gProfileId = 3;
   static private int gMaxFreq = -1;
   static private int gMinFreq = -1;
   static private String gFreqTable [] = null;
   public class cpufreq_table {
	   static final int F198 = 198*1000;
	   static final int F400 = 396*1000;
	   static final int F600 = 672*1000;
	   static final int F800 = 792*1000;
	   static final int F1G = 996*1000;

   }


   private int PERFORMANCE =1;
   private int POWERSAVING =2;
   private int DEFAULT =3;

   public class soc_type {
	   static final int MX6Q  = 63;
	   static final int MX6DL = 61;
   }
   public class soc_rev {
	   static final int TO1_0 = 10;
	   static final int TO1_1 = 11;
	   static final int TO1_2 = 12;
	   static final int TO1_3 = 13;
   }


   public void testProvider() {
	   if(DEBUG) Log.i(TAG,"testProvider");
	   ContentResolver cr = getContentResolver();
	   Uri uri = ContentUris.withAppendedId(PowerServiceDB.Profiles.CONTENT_URI, 1);
	   //Uri uri =
	   Cursor cur = cr.query(PowerServiceDB.Profiles.CONTENT_URI, null, null, null, null);
	   if(DEBUG) Log.i(TAG, "query the profiles table, create it");

   }

   @Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	public FSLPowerOptionService (){
		super();
	}

	@Override
	public void onCreate() {
	super.onCreate();
	mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	if(DEBUG) Log.i(TAG,"service is starting");
	timer = new Timer(true);
	String sys_rev = get_system_rev();
	if(!sys_rev.isEmpty()){
		gSocType = (int) Long.parseLong(sys_rev.substring(1, 3));
		Log.i(TAG, "get soc type " + gSocType +"string:"+sys_rev.substring(1,3));
	}
	else
		Log.e(TAG, "could not get SOC type");

	if(!sys_rev.isEmpty()){
		gSocRev = (int) Long.parseLong(sys_rev.substring(4));
		if(DEBUG) Log.i(TAG, "get soc revision " + gSocRev +"string:"+sys_rev.substring(4));
	}
	else
		Log.e(TAG, "could not get SOC revision");

	if (gSocType <=0 || gSocRev <=0)
		Log.e(TAG,"get the Soc type or Soc rev fail!");


	ContentResolver cr = getContentResolver();
	Cursor cur = cr.query(PowerServiceDB.Profiles.CONTENT_URI, null, null, null, null);
	int i  = cur.getCount();
	String freq_string = getFreqTable();
	String freq_table[] = freq_string.split(" ");
	gFreqTable = freq_table;
	if(DEBUG) {
		for(String freq : gFreqTable)
			Log.i(TAG,"get freq table "+freq);
	}
	gMaxFreq = (int)(Long.parseLong(freq_table[freq_table.length-1]))/1000;
	gMinFreq = (int)(Long.parseLong(freq_table[0]))/1000;
	if (i  == 0)
		initPowerProfile(gSocType, gSocRev);
	cur.close();
	cur = null;
	gMaxcpus = getCurCPUNm();
	if(DEBUG) Log.i(TAG,"boot with core0-"+gMaxcpus);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if(DEBUG) Log.i(TAG,"service is starting" + "  "+startId);


		//controlThermal(50,40);
		//if(DEBUG) Log.i(TAG,"set temp to 50,40 ");
		//Toast.makeText(this, "service start", Toast.LENGTH_SHORT).show();

		if (gSocType <=0 || gSocRev <=0){
			Toast.makeText(this, "profile stop", Toast.LENGTH_SHORT).show();
			return;
		}
		// MX6DL TO1.0 thermal is not be calibrated, so don't start profile service
		if (gSocType == soc_type.MX6DL && gSocRev < soc_rev.TO1_1){
			Toast.makeText(this, "mx6dl TO1.0 profile stop", Toast.LENGTH_SHORT).show();
			return;
		}

		TimerTask moniteProfile = new TimerTask() {
			public void run() {
				getCurrentTime();
				tempMoniter();
				if(DEBUG) Log.i(TAG,"scheduletime="+scheduletime);
				showNotification();
				FSLPowerOptionService.this.stopSelf();
			}
		};

		ContentResolver cr = getContentResolver();
		Cursor cur = cr.query(Profiles.CONTENT_URI, null, Profiles.PfofileStatus + "="+"1", null, null);
		long profileid =3;
		int status = 0;

		if (cur!=null){
			if (cur.getCount() > 0) {
				cur.moveToFirst();
				profileid = cur.getLong(cur.getColumnIndex(Profiles.ProfileID));
				status = cur.getInt(cur.getColumnIndex(Profiles.PfofileStatus));
			}
		}
		cur.close();
		cur = null;
		Log.i(TAG, "start with profile id "+profileid +" status " +status);
		gProfileId = profileid;
		if (status!=0 && profileid!=0)
			activeProfile(cr, profileid);
		else//active normal mode
			activeProfile(cr, DEFAULT);


		timer.schedule(moniteProfile, new Date(10), 10000);


	}
	 @Override
	  public void onDestroy() {
		 mNM.cancel(NOTIFICATION);
	    //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
	  }

	    /**
	     * Show a notification while this service is running.
	     */
	    private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = getText(R.string.start);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text,
			System.currentTimeMillis());

		Intent intent;
	    ComponentName toLaunch;
	    intent = new Intent(Intent.ACTION_VIEW);
	    toLaunch = new ComponentName("fsl.power.manager",
			"fsl.power.manager.powermanager");
	    intent.setComponent(toLaunch);

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.app_name),
			       text, contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	    }

		public void getCurrentTime() {
			final Calendar c = Calendar.getInstance();
			mYear = c.get(Calendar.YEAR);
			mMonth = c.get(Calendar.MONTH);
			mDay = c.get(Calendar.DAY_OF_MONTH);
			mHour = c.get(Calendar.HOUR_OF_DAY);
			mMinute = c.get(Calendar.MINUTE);
			if(DEBUG) Log.i(TAG, "CurrentItme" + mYear + "-" + mMonth + "-" + mDay + "-"
					+ mHour + ":" + mMinute);

		}

		static String get_system_rev() {
			String  revision = null;
			String version = null;
			String tmp[] = null;
			try {
				BufferedReader reader = new BufferedReader(
						     new InputStreamReader(
						 new FileInputStream( "/proc/cpuinfo") ), 1000 );
				version = reader.readLine();
				while (version != null){
					if(DEBUG) Log.i(TAG,"get line: " +version);
					version = reader.readLine();
					if (version.startsWith("Revision")){
						tmp = version.split(":");
						//revision = (int)(Long.parseLong(tmp[1]));
						revision = tmp[1];
						break;
					}
				}
				reader.close();
			}catch( IOException ex ) {
				ex.printStackTrace();
			}
			tmp = null;
			if (DEBUG) Log.i(TAG, "get system revison " + revision);
			return revision;

		}


		static void setCPUFreq(int maxf, int minf){
			FileOutputStream scaling_max = null;
			File scaling_max1 = null;
			if (maxf > 0){
				scaling_max1 = new File(
						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");

				try {
					scaling_max = new FileOutputStream(scaling_max1);;
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {

					String freqs =  String.format("%d",maxf);
					if(DEBUG) Log.i(TAG, "write  max freq: " + maxf);
					scaling_max.write(freqs.getBytes());
					//scaling_max.write(freq);
					scaling_max.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (minf >= 0 ){
				scaling_max1 = new File(
						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");

				try {
					scaling_max = new FileOutputStream(scaling_max1);;
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {

					String freqs =  String.format("%d",minf);
					scaling_max.write(freqs.getBytes());
					//scaling_max.write(freq);
					scaling_max.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		static String getFreqTable(){
			String load = null;
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
							new FileInputStream(
					"/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies" )
							), RDSIZE );
				load = reader.readLine();
				String freq_table[] = load.split(" ");
				int minf = (int)(Long.parseLong(freq_table[freq_table.length-1]))/1000;
				int maxf = (int)(Long.parseLong(freq_table[0]))/1000;
				Log.i(TAG, "scaling cpu freqs:"+load+", maxf:"+maxf +", minf:"+minf);
				 reader.close();
			}catch( IOException ex ){
				ex.printStackTrace();
			}
			return load;

		}

		static void setCPUGovernor(String gover)
		{
			FileOutputStream scaling_max = null;
			File scaling_max1 = null;
				scaling_max1 = new File(
						"/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");

				try {
					scaling_max = new FileOutputStream(scaling_max1);;
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					scaling_max.write(gover.getBytes());
					scaling_max.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


		}
		static void offlineCPU(int id, boolean on){

			FileOutputStream cpuhotlug = null;
			File cpuhotlug1 = new File(
					"sys/devices/system/cpu/cpu"+id+"/online");

			try {
				cpuhotlug = new FileOutputStream(cpuhotlug1);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (on){
					String up =  String.format("1");
					cpuhotlug.write(up.getBytes());
				} else {
					String off =  String.format("0");
					cpuhotlug.write(off.getBytes());
				}

				cpuhotlug.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		static void adjustCPUFreq(int curfreq, boolean up){
			if(DEBUG) Log.i(TAG,"cur_cpufreq  "+ curfreq);
			if (up)
				setCPUFreq(cpufreq_table.F1G, cpufreq_table.F400);
			else  {
				switch (curfreq*1000){

				case cpufreq_table.F400:

					setCPUFreq(cpufreq_table.F400, cpufreq_table.F400);
					break;
				case cpufreq_table.F600:

					setCPUFreq(cpufreq_table.F400, cpufreq_table.F400);
					break;
				case cpufreq_table.F800:

					setCPUFreq(cpufreq_table.F600, cpufreq_table.F400);
					break;
				case cpufreq_table.F1G:

					setCPUFreq(800*1000, cpufreq_table.F400);
					break;
				case cpufreq_table.F198:
				default:
						Log.i(TAG,"do nothing");

				}
			}
		}

		static int getCurCPUNm(){
	    //sys/devices/system/cpu/online
			/* need root permission to change the cpuinfo_cur_freq */
	    int cpuNm = 0;
	    try {
		  BufferedReader reader = new BufferedReader(
						     new InputStreamReader(
							   new FileInputStream( "/sys/devices/system/cpu/online" ) ), 1000 );
		  String load = reader.readLine();
		  if (load.length()==1)
			  cpuNm = (int)(Long.parseLong(load));
		  else{
			  String cpunum = load.substring(2);
			  cpuNm = (int)(Long.parseLong(cpunum));
		  }

		  reader.close();

		 }catch( IOException ex )
		 {
			ex.printStackTrace();
		 }
	    return cpuNm ;

		}

		static void controlThermal(int hottemp, int activetemp){
			FileOutputStream thermal_hot = null;
			File thermal_hot1 = new File(
					"/sys/class/thermal/thermal_zone0/trip_point_1_temp");

			try {
				thermal_hot = new FileOutputStream(thermal_hot1);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {

				String hot =  String.format("%d",hottemp);
				thermal_hot.write(hot.getBytes());
				thermal_hot.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			FileOutputStream thermal_active = null;
			File thermal_active1 = new File(
					"/sys/class/thermal/thermal_zone0/trip_point_2_temp");

			try {
				thermal_active = new FileOutputStream(thermal_active1);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				//thermal_active.write(activetemp);
				String act =  String.format("%d",activetemp);
				thermal_active.write(act.getBytes());
				thermal_active.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		private  void tempMoniter() {
			/* Monitor the system temp to keep it
			 * at MAX_TEMP
			 */
			int cpu_freq = getCPUCurrentFreq();
			int soc_temp = getCPUTemp();
			int cpu_nm = getCurCPUNm();
			if(DEBUG) Log.i(TAG,"cpu frequency= "+cpu_freq+ " profileId ="
					+ gProfileId+ " soc temp="+soc_temp+ "  CPU nM "+cpu_nm);

			if (gProfileId == PERFORMANCE){
				if(DEBUG) Log.i(TAG, " tempMoniter cancel operation due to run in performance mode");
				return;
			}

			if(gNothermal){
				if (soc_temp >= gTempKeeper){
					// lower the cpu freq
					if(DEBUG) Log.i(TAG,"lowe the cpu freq and cpu nm");
					adjustCPUFreq(cpu_freq,false);
					//offline the cpu core
					if (cpu_nm > 0) {
						offlineCPU(cpu_nm,false);
						if(DEBUG) Log.i(TAG,"offline cpu "+ cpu_nm);
					}

				}else{
					//increase the cpu freq
					if (cpu_freq < cpufreq_table.F1G){
						adjustCPUFreq(cpu_freq,true);
					}
					//bring up cpu cores
					if (cpu_nm < gMaxcpus) {
						offlineCPU(cpu_nm+1,true);
						if(DEBUG) Log.i(TAG,"bring up cpu "+ cpu_nm);
					}
				}
			}else {

				/* use thermal to control the cpu freq */
				if (soc_temp >= gTempKeeper){
					if (getCPUCurrentFreq() < cpufreq_table.F400)
						setCPUFreq(0, cpufreq_table.F400);
					if(DEBUG) Log.i(TAG,"decrease performance ");
					if (cpu_nm > 0) {
						offlineCPU(cpu_nm,false);
						if(DEBUG) Log.i(TAG,"offline cpu "+ cpu_nm);
					}

				}else{
					//controlThermal(80,60);
					if(DEBUG) Log.i(TAG,"increase performance ");
					if (cpu_nm < gMaxcpus) {
						offlineCPU(cpu_nm+1,true);
						if(DEBUG) Log.i(TAG,"bring up cpu "+ cpu_nm);
					}
				}
			}


		}

		static int getCPUCurrentFreq(){
			    //sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq
			/* need root permission to change the cpuinfo_cur_freq */
			    int cpuFreq = 0;
			    try {
				  BufferedReader reader = new BufferedReader(
								     new InputStreamReader(
									   new FileInputStream(
											   "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq" )
									   ), 1000 );
				  String load = reader.readLine();
				  cpuFreq = (int)(Long.parseLong(load))/1000;
				  //if(DEBUG) Log.i(TAG, "cpuinfo_cur_freq:"+load+", cpuFreq:"+cpuFreq);
				  reader.close();
				 }catch( IOException ex )
				 {
					ex.printStackTrace();
				 }
			    return cpuFreq;
		    }


		static int getCPUTemp(){
	    //sys/class/thermal/thermal_zone0/temp
	  int temp = 0;
	    try {
		  BufferedReader reader = new BufferedReader(
						     new InputStreamReader(
							   new FileInputStream( "/sys/class/thermal/thermal_zone0/temp" ) ), 1000 );
		  String load = reader.readLine();
		  temp = (int)(Long.parseLong(load));
		  reader.close();
		 }catch( IOException ex )
		 {
			ex.printStackTrace();
		 }
	    return temp;
		}

		public void initPowerProfile(int soctype, int socrev){

			   ContentResolver cr = getContentResolver();
			   ContentValues values = new ContentValues();
			   //Uri uri = ContentUris.withAppendedId(PowerServiceDB.Profiles.CONTENT_URI, 1);
			   //Cursor cur = cr.query(PowerServiceDB.Profiles.CONTENT_URI, null, null, null, null);
			   if (soctype == soc_type.MX6Q){

				   values.clear();
				   values.put(Profiles.ProfileName, "Performance Mode");
				   values.put(Profiles.ProfileID, PERFORMANCE);
				   values.put(Profiles.TempActive, "60");
				   values.put(Profiles.TempHot, "80");
				   values.put(Profiles.MaxFreq, gFreqTable[0]);
				   values.put(Profiles.MinFreq, gFreqTable[0]);
				   values.put(Profiles.CPUNM, "3");
				   values.put(Profiles.CPUHotPlug, "0");
				   values.put(Profiles.CurCPUGov, "performance");
				   values.put(Profiles.PfofileStatus, "0");
				   cr.insert(Profiles.CONTENT_URI, values);

				   values.clear();
				   values.put(Profiles.ProfileName, "PowerSaving Mode");
				   values.put(Profiles.ProfileID, POWERSAVING);
				   values.put(Profiles.TempActive, "60");
				   values.put(Profiles.TempHot, "80");
				   values.put(Profiles.MaxFreq, gFreqTable[1]);
				   values.put(Profiles.MinFreq, gFreqTable[gFreqTable.length-1]);
				   values.put(Profiles.CPUNM, "0");
				   values.put(Profiles.CPUHotPlug, "1");
				   values.put(Profiles.CurCPUGov, "ondemand");
				   values.put(Profiles.PfofileStatus, "0");
				   cr.insert(Profiles.CONTENT_URI, values);

				   values.clear();
				   values.put(Profiles.ProfileName, "Normal Mode");
				   values.put(Profiles.ProfileID, DEFAULT);
				   values.put(Profiles.TempActive, "50");
				   values.put(Profiles.TempHot, "70");
				   values.put(Profiles.MaxFreq, gFreqTable[0]);
				   values.put(Profiles.MinFreq, gFreqTable[gFreqTable.length-1]);
				   values.put(Profiles.CPUNM, "1");
				   values.put(Profiles.CPUHotPlug, "1");
				   values.put(Profiles.CurCPUGov, "interactive");
				   values.put(Profiles.PfofileStatus, "1");
				   cr.insert(Profiles.CONTENT_URI, values);
			   }

			   if (soctype == soc_type.MX6DL){

				   values.clear();
				   values.put(Profiles.ProfileName, "Performance Mode");
				   values.put(Profiles.ProfileID, PERFORMANCE);
				   values.put(Profiles.TempActive, "60");
				   values.put(Profiles.TempHot, "80");
				   values.put(Profiles.MaxFreq, gFreqTable[0]);
				   values.put(Profiles.MinFreq, gFreqTable[0]);
				   values.put(Profiles.CPUNM, "1");
				   values.put(Profiles.CPUHotPlug, "0");
				   values.put(Profiles.CurCPUGov, "performance");
				   values.put(Profiles.PfofileStatus, "0");
				   cr.insert(Profiles.CONTENT_URI, values);

				   values.clear();
				   values.put(Profiles.ProfileName, "PowerSaving Mode");
				   values.put(Profiles.ProfileID, POWERSAVING);
				   values.put(Profiles.TempActive, "60");
				   values.put(Profiles.TempHot, "80");
				   values.put(Profiles.MaxFreq, gFreqTable[1]);
				   values.put(Profiles.MinFreq, gFreqTable[gFreqTable.length-1]);
				   values.put(Profiles.CPUNM, "0");
				   values.put(Profiles.CPUHotPlug, "0");
				   values.put(Profiles.CurCPUGov, "ondemand");
				   values.put(Profiles.PfofileStatus, "0");
				   cr.insert(Profiles.CONTENT_URI, values);

				   values.clear();
				   values.put(Profiles.ProfileName, "Normal Mode");
				   values.put(Profiles.ProfileID, DEFAULT);
				   values.put(Profiles.TempActive, "50");
				   values.put(Profiles.TempHot, "70");
				   values.put(Profiles.MaxFreq, gFreqTable[0]);
				   values.put(Profiles.MinFreq, gFreqTable[gFreqTable.length-1]);
				   values.put(Profiles.CPUNM, "1");
				   values.put(Profiles.CPUHotPlug, "1");
				   values.put(Profiles.CurCPUGov, "interactive");
				   values.put(Profiles.PfofileStatus, "1");
				   cr.insert(Profiles.CONTENT_URI, values);
			   }

		}

		static int activeProfile(ContentResolver cr, long profileid){
			if(DEBUG) Log.i(TAG,"active proileid " + profileid);
			long actid = profileid;
			long oldid = gProfileId;
			int hottemp = 80;
			int acttemp	= 60;
			int maxfreq = gMaxFreq*1000;
			int minfreq = gMinFreq*1000;
			String goveron = "performance";
			int cpunm = 3;
			int status =0;
			int hotplug = 1;

			//get the profile prarms from sql

			//ContentResolver cr = getContentResolver();
			Uri uri = ContentUris.withAppendedId(Profiles.CONTENT_URI,profileid);
			Cursor cur;
			cur = cr.query(uri, null, null, null, null);
			if (cur !=null){
				if (cur.getCount() > 0) {
					cur.moveToFirst();
					actid = cur.getLong(cur.getColumnIndex(Profiles.ProfileID));
					hottemp = cur.getInt(cur.getColumnIndex(Profiles.TempHot));
					acttemp = cur.getInt(cur.getColumnIndex(Profiles.TempActive));
					cpunm = cur.getInt(cur.getColumnIndex(Profiles.CPUNM));
					hotplug = cur.getInt(cur.getColumnIndex(Profiles.CPUHotPlug));
					maxfreq = cur.getInt(cur.getColumnIndex(Profiles.MaxFreq));
					minfreq = cur.getInt(cur.getColumnIndex(Profiles.MinFreq));
					goveron = cur.getString(cur.getColumnIndex(Profiles.CurCPUGov));

				}
			}
			cur.close();
			cur = null;
			cur = cr.query(Profiles.CONTENT_URI, null, Profiles.PfofileStatus + "="+"1", null, null);
			if (cur !=null){
				if (cur.getCount() > 0) {
					cur.moveToFirst();
					oldid = cur.getInt(cur.getColumnIndex(Profiles.ProfileID));
				}
			}
			cur.close();
			cur = null;
			ContentValues valuestmp = new ContentValues();
		valuestmp.clear();
		valuestmp.put(Profiles.PfofileStatus,0);
		cr.update(Profiles.CONTENT_URI, valuestmp, Profiles.ProfileID+"="+oldid, null);
		if(DEBUG) Log.i(TAG, "clean profile " + oldid +" status"+" max freq ="+maxfreq);
		valuestmp.clear();
		valuestmp.put(Profiles.PfofileStatus,1);
		cr.update(Profiles.CONTENT_URI, valuestmp, Profiles.ProfileID+"="+actid, null);
		if(DEBUG) Log.i(TAG, "update profile " + oldid +" status");
		valuestmp.clear();
		valuestmp = null;
		if(DEBUG) Log.i(TAG,"active profile "+ actid+ "/ " + hottemp+ " / " +acttemp+ " / " +cpunm + " / " +hotplug +" / " +maxfreq+" / "  +minfreq +" / " +goveron);

		controlThermal(hottemp,acttemp);
		setCPUFreq(maxfreq, minfreq);
		setcpuNM(cpunm);
		setCPUGovernor(goveron);
		//setGPUFPS(gpufps);
		gProfileId = actid;
		gTempKeeper = hottemp;
		gMaxcpus = cpunm;


			return 0;

		}

		static int setcpuNM(int cpunm){
			int curnm = getCurCPUNm();
			if (cpunm > curnm) {
				while (cpunm > curnm){
					curnm++;
					offlineCPU(curnm, true);
				}
			} else {
				while (curnm >cpunm){
					offlineCPU(curnm, false);
					curnm--;

				}
			}
			return getCurCPUNm();
		}



}
