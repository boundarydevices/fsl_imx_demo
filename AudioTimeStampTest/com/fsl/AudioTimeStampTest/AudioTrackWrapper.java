/*
 * Copyright (C) 2014 Freescale Semiconductor, Inc.
 */


package com.genius.audio.track;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.media.AudioTimestamp;

public class AudioTrackWrapper {
	
	int mFrequency;					// samplerate
	int mChannel;					// channel
	int mSampBit;					// samplebit
    int mChanCount;
	int mSampleBits;
	int i=0;
	int totalPcmLen;
	long preTimeStampNs;
	long curFeedTimeStampNs;
	long mNanoTime;
	long preframePosition = 0;
	long deltaframe;
	long deltatimeMs;
	long min_deltatimeMs;
	long max_deltatimeMs;
	long sum=0;
	AudioTrack mAudioTrack;			
	
	public AudioTrackWrapper(int frequency, int channel, int sampbit){
		mFrequency = frequency;
		mChannel = channel;
		mSampBit = sampbit;
	}

	public void init(){
		if (mAudioTrack != null){
			release();
		}
		int minBufSize = AudioTrack.getMinBufferSize(mFrequency, mChannel, mSampBit);
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 
										mFrequency, 
										mChannel, 
										mSampBit, 
										minBufSize,
										AudioTrack.MODE_STREAM);
		
		if(mChannel == AudioFormat.CHANNEL_CONFIGURATION_MONO)
		   mChanCount = 1;
		else if(mChannel == AudioFormat.CHANNEL_CONFIGURATION_STEREO)
		   mChanCount = 2;
		else
		   System.out.println("mChanCount is wrong");
		
		if(mSampBit==AudioFormat.ENCODING_PCM_8BIT)
		   mSampleBits=1;
		else if(mSampBit==AudioFormat.ENCODING_PCM_16BIT)
		   mSampleBits=2;
		else
			System.out.println("mSampleBits is wrong");
		
		   mAudioTrack.play();	
	}

	public void release(){
		if (mAudioTrack != null){
			mAudioTrack.stop();				       	
			mAudioTrack.release();
		    }
	    }
	
	public void playAudioTrack(byte []data, int offset, int length){
		if (data == null || data.length == 0){
			return ;
		}
		
//		  AudioTimestamp mAudioTimestamp=new AudioTimestamp();
//		  mAudioTrack.getTimestamp(mAudioTimestamp);
		//System.out.println("-----------no any data, cur played frams ---------"+mAudioTimestamp.framePosition);
		
		try {
			mAudioTrack.write(data, offset, length);
			totalPcmLen=totalPcmLen+length;
			AudioTimestamp mAudioTimestamp=new AudioTimestamp();
			mAudioTrack.getTimestamp(mAudioTimestamp);
            long curFeedFrames = (long)totalPcmLen/(mChanCount*mSampleBits);
			if(preframePosition>mAudioTimestamp.framePosition){
				 System.out.println(" stamp increase err ");
	       	 }
            System.out.println("----------- mAudioTimestamp.framePosition ---------"+mAudioTimestamp.nanoTime);
			preframePosition=mAudioTimestamp.framePosition;
			if(curFeedFrames < mAudioTimestamp.framePosition) {
				 System.out.println(" stamp err ");
				 System.out.println("----------- cur feed frams ---------"+curFeedFrames);
				 System.out.println("----------- cur played frams ---------"+mAudioTimestamp.framePosition);
			 }
			deltaframe=curFeedFrames-mAudioTimestamp.framePosition;
			deltatimeMs=deltaframe*1000/mFrequency;
			if (i==0){
				 min_deltatimeMs=deltatimeMs;
				 max_deltatimeMs=deltatimeMs;
		     }
			if(deltatimeMs<min_deltatimeMs)
				 min_deltatimeMs=deltatimeMs;
			if(deltatimeMs>max_deltatimeMs)
				 max_deltatimeMs=deltatimeMs;
			     sum+=deltatimeMs;
			     i++;
		    } catch (Exception e) {
			 // TODO: handle exceptionS
			 Log.i("MyAudioTrack", "catch exception...");
		}
	}
	
	public long ave_TiemMs(){
		if(i==0)return 0;
		else return sum/i;
	}
	public long min_TimeMs(){
		return min_deltatimeMs;
	}
	public long max_TimeMs(){
		return max_deltatimeMs;
	}

	public int getPrimePlaySize(){
		int minBufSize = AudioTrack.getMinBufferSize(mFrequency, mChannel, mSampBit);
		return minBufSize * 2;
	}
}


