/*
 * Copyright (C) 2014 Freescale Semiconductor, Inc.
 */


package com.genius.audio.track;

import java.util.Timer;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;

public class AudioPlayThread implements Runnable{
	final int EVENT_PLAY_OVER = 0x100;
	final int EVENT_PLAY_STOP = 0x200;
	final int EVENT_AVETIME=0x300;
	final int EVENT_MINTIME=0x400;
	final int EVENT_MAXTIME=0x500;
	long my_aveTime;
	long my_minTime;
	long my_maxTime;
	byte []data;
	Handler mHandler;
	int mFrequency,mChannel,mSampbit;
	int mChannel_macro = 0;
	int mSampbit_macro=0;
	
	public AudioPlayThread(byte []data, Handler handler,int frequency, int channel, int sampbit) {
		// TODO Auto-generated constructor stub
		this.data = data;
		mHandler = handler;
		mFrequency=frequency;
		mChannel=channel;
		mSampbit=sampbit;
	}
	
	public void run() {
		Log.i("MyThread", "run..");
		if (data == null || data.length == 0){
			return ;
		}
              
 		if(mChannel==1)
 			mChannel_macro=AudioFormat.CHANNEL_CONFIGURATION_MONO;
 		else if(mChannel==2)
 			mChannel_macro=AudioFormat.CHANNEL_CONFIGURATION_STEREO;
 	    if(mSampbit==1)
 			mSampbit_macro=AudioFormat.ENCODING_PCM_8BIT;
 		else if(mSampbit==2)
 			mSampbit_macro=AudioFormat.ENCODING_PCM_16BIT;
 		AudioTrackWrapper myAudioTrack = new AudioTrackWrapper(mFrequency, mChannel_macro, mSampbit_macro);
      	myAudioTrack.init();
		int playSize = myAudioTrack.getPrimePlaySize();
		Log.i("MyThread", "total data size = " + data.length + ", playSize = " + playSize);
		int index = 0;
		int offset = 0;
		Message msg = null;
		while(true){
			try {
				 Thread.sleep(0);
				 offset = index * playSize;
				 if(offset>=data.length){
					System.out.println("finish3");
				    msg = Message.obtain(mHandler, EVENT_PLAY_OVER);
				    mHandler.sendMessage(msg);
					break;
				  }
				 myAudioTrack.playAudioTrack(data, offset, playSize);
			 } catch (Exception e) {
					// TODO: handle exception
				    break;
		}
			index++;
			if (index >= data.length){
				break;
			}
	}
		my_aveTime=myAudioTrack.ave_TiemMs();
		my_minTime=myAudioTrack.min_TimeMs();
		my_maxTime=myAudioTrack.max_TimeMs();
	    if (msg==null){
		    msg=Message.obtain(mHandler,EVENT_PLAY_STOP) ;
			mHandler.sendMessage(msg);
		}
	    for(int i=0;i<=100;i++);
		msg=Message.obtain(mHandler,EVENT_AVETIME,my_aveTime);
		System.out.println("----thread-my_aveTime-----"+msg.obj);
		mHandler.sendMessage(msg);
		for(int i=0;i<=100;i++);
		msg=Message.obtain(mHandler,EVENT_MINTIME,my_minTime);
		System.out.println("----thread-my_minTime-----"+my_minTime);
		mHandler.sendMessage(msg);
		for(int i=0;i<=100;i++);
		msg=Message.obtain(mHandler,EVENT_MAXTIME,my_maxTime);
		System.out.println("----thread-my_maxTime-----"+my_maxTime);
		mHandler.sendMessage(msg);
		myAudioTrack.release();
	}
}
