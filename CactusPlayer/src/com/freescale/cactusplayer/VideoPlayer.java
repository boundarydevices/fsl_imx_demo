/* Copyright 2012-2013 Freescale Semiconductor, Inc.
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


package com.freescale.cactusplayer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
//import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SeekBar;
import android.widget.TextView;
//import android.widget.Toast;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.view.View.OnClickListener;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.AudioManager;
import java.io.IOException;
import android.os.Parcel;
import android.app.ActionBar;
//import android.view.MenuInflater;
import android.media.TimedText;

public class VideoPlayer extends Activity implements SeekBar.OnSeekBarChangeListener, SurfaceHolder.Callback {
	private static final String TAG   = "Cactus";
	private static final String CLASS = "VideoPlayer: ";

	private static final int PLAYER_PLAYING = 0;
	private static final int PLAYER_PAUSED  = 1;
	private static final int PLAYER_STOPPED = 2;
	private static final int PLAYER_EXITED  = 3;

    //------------------------------------------------------------------------------------
    // widgets
    //------------------------------------------------------------------------------------
    private MediaPlayer mMediaPlayer = null;
	//private   SurfaceHolder mPlayerSurfaceHolder = null;
    private int         mVideoWidth;
    private int         mVideoHeight;
    private int         mSpeed = 1;
    private boolean     mDragging;
    private int         mAudioTrackCount = 0;
    private int         mSubtitleTrackCount = 0;
    private int         mCurSubtitleTrack = -1;
    private int         mCurSubtitleIndex = -1;
    private int         mCurAudioTrack = -1;
    private int         mCurAudioIndex = -1;
    private boolean     mLoopFile = false;

    // video display
	private   VideoView    mVideoView;
	private   SurfaceHolder mSurfaceHolder = null;

    // subtitle
	private   AutoHideTextView    mSubtitleTextView;
	private   AutoHideTextView    mSubtitleInfoView;

    // track, ff/rw information
	private   AutoHideTextView    mInfoView;

	private   AutoHideTextView    mTrackInfoView;

    // error sign
	private   AutoHideTextView    mErrSign;

    // control bar and seek bar
	private   ImageButton mBtnFastBack;
	private   ImageButton mBtnPlayPause;
	private   ImageButton mBtnFastForward;
	private   TextView    mCurPosView;
	private   TextView    mEndPosView;
	private   SeekBar     mProgressBar;

    // waiting dialog
	private Dialog mWaitingDialog = null;

	private PowerManager.WakeLock wl = null;

    //------------------------------------------------------------------------------------
    // properties (url, speed, state, etc.)
    //------------------------------------------------------------------------------------
	private   String      mUrl       = null;
	private   int         mPlayState = PLAYER_PLAYING;
	private   float       mPlaySpeed = 1;
	private   long        mDuration  = 0; // cache of file duration and current playback position
	private   long        mCurPos    = 0;
	private   boolean     mSubtitleEnabled = true;
	private   long        mTimeOffset = 0;

    //------------------------------------------------------------------------------------
    // constants
    //------------------------------------------------------------------------------------
	private static final int UPDATE_PLAYBACK_STATE  = 1;
	private static final int UPDATE_PLAYBACK_SPEED  = 2;
	private static final int UPDATE_PLAYBACK_PROGRESS = 4;
	private static final int UPDATE_PLAYBACK_ERROR  = 8;

	private static final int LOOP_FILE  = Menu.FIRST+1;
	private static final int SELECT_AUDIO    = Menu.FIRST+2;
	private static final int SELECT_SUBTITLE  = Menu.FIRST+3;
	private static final int CLOSE_SUBTITLE  = Menu.FIRST+4;

	private long progressBarProhibitTime = -1;

	private String locale = "eng";

    private static final int    SHOW_PROGRESS = 1;

    private final static String IMEDIA_PLAYER = "android.media.IMediaPlayer";
    private static final int INVOKE_ID_SELECT_TRACK = 4;

	private void parseIntent(Intent intent) {
		Log.d(TAG, CLASS + "parseIntent");
		if(intent == null) {
			return;
		}

		Uri uri = intent.getData();
		if(uri != null) {
			// set url to load
			mUrl = uri.toString();

			// display waiting dialog if possible
			/*
			if(mWaitingDialog == null)
			{
				mWaitingDialog = new Dialog(this, android.R.style.Theme_Translucent);
				mWaitingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				mWaitingDialog.setContentView(R.layout.myprogressdlg);
				mWaitingDialog.setCancelable(true); // just a indication; no need to block user operations
				mWaitingDialog.setCanceledOnTouchOutside(false);
				mWaitingDialog.show();
			}
			*/
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, CLASS + "onCreate");
		super.onCreate(savedInstanceState);

		boolean isRestored = (savedInstanceState != null);

		//loadNativeLibs();
		initLocaleTable();

		setContentView(R.layout.scaleplayer);

		mVideoView = (VideoView)findViewById(R.id.SurfaceView);
		mSurfaceHolder = mVideoView.getHolder();
		if(mSurfaceHolder != null)
			mSurfaceHolder.addCallback(this);

		mBtnFastBack     = (ImageButton) findViewById(R.id.fastback);
		mBtnPlayPause    = (ImageButton) findViewById(R.id.playpause);
		mBtnFastForward  = (ImageButton) findViewById(R.id.fastforward);
		mBtnFastBack	 .setOnClickListener(mOnBtnClicked);
		mBtnPlayPause	 .setOnClickListener(mOnBtnClicked);
		mBtnFastForward	 .setOnClickListener(mOnBtnClicked);

		mCurPosView      = (TextView)    findViewById(R.id.currentpos);
		mEndPosView    = (TextView)    findViewById(R.id.duration);

		mBtnPlayPause.setBackgroundResource(R.drawable.play);

		mInfoView        = (AutoHideTextView)    findViewById(R.id.info);
		mInfoView.setVisibility(View.INVISIBLE);

		mSubtitleTextView  = (AutoHideTextView)    findViewById(R.id.subtitletext);
		mSubtitleTextView.setVisibility(View.INVISIBLE);

		mSubtitleInfoView  = (AutoHideTextView)    findViewById(R.id.subtitleinfo);
		mSubtitleInfoView.setVisibility(View.INVISIBLE);

		mProgressBar     = (SeekBar)      findViewById(R.id.seek);
		mProgressBar     .setOnSeekBarChangeListener(this);

/**
		setJavaContext(this);

		mJniHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Bundle bundle = msg.getData();
				if(bundle != null)
					OnDataSourceMessage(bundle);
			}
		};
**/
		Intent intent = getIntent();
		parseIntent(intent);

	}

	@Override
	protected void onNewIntent (Intent intent) {
		Log.d(TAG, CLASS + "onNewIntent");

        // if video view not ready, just record the smil file
        if(mVideoView.getSurface() == null) {
			parseIntent(intent);
			return;
    	}

        // stop current player
		stop();
		mUrl = null;

        // get new file path
		parseIntent(intent);

	}

    @Override
	protected void onStart() {
	    Log.d(TAG, CLASS + "onStart");
		super.onStart();

		// keep screen bright
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Cactus Player");
		wl.acquire();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, CLASS + "onResume");
		super.onResume();
		////play();
		start();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, CLASS + "onPause");
		super.onPause();
		pause();
        if(mPlaySpeed != 1)
            setNormalSpeed();
	}

    @Override
	protected void onStop() {
		Log.d(TAG, CLASS + "onStop");
		super.onStop();
		//stop();
		//mUrl = null;

		// stop screen bright
		wl.release();

		// clean gui to not disturb onStart() next time
		//initPlay();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, CLASS + "onDestroy");


		super.onDestroy();
	}


    //------------------------------------------------------------------------------------
	// Update GUI
    //------------------------------------------------------------------------------------

    private void updateButtons(int reason, long val1, long val2, float val3) {
        if(reason == UPDATE_PLAYBACK_SPEED)
        {
		    float newSpeed = val3;
			if(newSpeed == 1)
			{
    			mInfoView.setText(null, 0);
			}
			else
			{
				// trick play
				String text;
				if(newSpeed > 0f)
					text = "FF X" + newSpeed;
				else
					text = "RW X" + (-newSpeed);

				mInfoView.setText(text, -1);
        		mSubtitleTextView.setText(null,0);
			}
        }
        else if(reason == UPDATE_PLAYBACK_STATE)
        {
            mPlayState = (int) val1;
            //Log.d(TAG,"play state changed to " + mPlayState);
        }
        else if(reason == UPDATE_PLAYBACK_PROGRESS)
        {
		    /**
			if(val3 == 0) { // no video frame updated, maybe this event can be discarded
				if(progressBarProhibitTime > 0) {
					long curTime = System.currentTimeMillis();
					if(curTime - progressBarProhibitTime < 2000)
						return;

					progressBarProhibitTime = -1;
				}
			}
			**/

			if(val1 > 0)
				mDuration = val1;
			else if(mDuration == 0)
				mDuration = mMediaPlayer.getDuration();

			val2 -= mTimeOffset; // offset by mTimeOffset

			if(val2 > mDuration)
				val2 = mDuration;
			else if(val2 < 0)
				val2 = 0;

			mCurPos   = val2;

			long seconds = mDuration / 1000;
			long minutes = seconds / 60;
			seconds = seconds - minutes * 60;
			long hours = minutes / 60;
			minutes = minutes - hours * 60;
			String s = "";

			if(hours < 10)
				s += "0";
			s += hours + ":";

			if(minutes < 10)
				s += "0";
			s += minutes + ":";

			if(seconds < 10)
				s += "0";
			s += seconds;
			mEndPosView.setText(s);

			seconds = mCurPos / 1000;
			minutes = seconds / 60;
			seconds = seconds - minutes * 60;
			hours = minutes / 60;
			minutes = minutes - hours * 60;
			s = "";

			if(hours < 10)
				s += "0";
			s += hours + ":";

			if(minutes < 10)
				s += "0";
			s += minutes + ":";

			if(seconds < 10)
				s += "0";
			s += seconds;
			mCurPosView.setText(s);

			if(mDuration > 0) {
				int pos = (int) (mCurPos/1000*1000 * 100 / mDuration);
				if(pos < 0)
					pos = 0;
				else if(pos > 100)
					pos = 100;
				mProgressBar.setProgress(pos);
			}
			else {
				mProgressBar.setProgress(0);
			}
        }
        else if(reason == UPDATE_PLAYBACK_ERROR)
        {
		    // show error sign
			//mErrSign.setVisibility(View.VISIBLE);

			// dismiss waiting dialog
			if(mWaitingDialog != null)
			{
				mWaitingDialog.dismiss();
				mWaitingDialog = null;
			}
        }

        if(mPlayState == PLAYER_EXITED) { // exit
            finish();
        }

        // now update buttons/progressbar
        boolean enablefb = true;
        boolean enableplaypause = true;
        boolean enableff = true;
        boolean enablestop = true;
        boolean enablesub = true;
        boolean enableseek = true;
        boolean enableplay = true;

        if(mUrl == null) {
			enablefb = false;
			enableplaypause = false;
			enableff = false;
			enablestop = false;
			enablesub = false;
			enableseek = false;
			enableplay = false;
        }

        //Log.d(TAG,"current play state is " + mPlayState);

        if(mPlayState == PLAYER_PLAYING)
            mBtnPlayPause.setBackgroundResource(R.drawable.pause);
        else // paused or stopped
            mBtnPlayPause.setBackgroundResource(R.drawable.play);

        if(mPlayState == PLAYER_STOPPED) { // stopped: BOF or EOF or user stopped
//				finish(); // back to storefront, according to DivX requirement
			// need to distinguish the reason
			enablefb = false;
			enableplaypause = true;
			enableff = false;
			enablestop = true;
			enablesub = false;
			enableseek = false;
			enableplay = true;
        }
        else { // 0 - playing; 1 - paused
			if(mPlaySpeed != 1)
				enableseek = false;
        }

        if(mPlayState == PLAYER_PAUSED || mPlayState == PLAYER_STOPPED)
            pauseMetadataShow();
        else
            resumeMetadataShow();

        mBtnFastBack   .setEnabled(enablefb);
        mBtnPlayPause  .setEnabled(enableplaypause);
        mBtnFastForward.setEnabled(enableff);
        mProgressBar   .setEnabled(enableseek);
    }

    //------------------------------------------------------------------------------------
	// Playback control
    //------------------------------------------------------------------------------------

    private void initPlay() {
		mPlayState   = PLAYER_STOPPED;
		mPlaySpeed   = 0;
		mDuration    = 0;
		mCurPos      = 0;
		mTimeOffset  = 0;

		resumeMetadataShow();

		// init gui

		if(mWaitingDialog != null)
		{
			mWaitingDialog.dismiss();
			mWaitingDialog = null;
		}

		mInfoView.setText(null, 0);
        mSubtitleTextView.setText(null,0);
        mSubtitleTextView.setText(null,0);
		mEndPosView.setText(null);
		mCurPosView.setText(null);
		mProgressBar.setProgress(0);
        mCurSubtitleIndex = -1;
        mCurSubtitleTrack = -1;

		updateButtons(-1, 0, 0, 0);
	}


    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {

            start();
        }
    };

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
        new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                Log.d(TAG,"onVideoSizeChanged");
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();

                //if(mVideoWidth >= 1280 || mVideoHeight >= 720) {
                //    mCurrentState = STATE_ERROR;
                //    mTargetState = STATE_ERROR;
                //    VideoResNotSupportWindow();
                //    return;
                //}

                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    Log.d(TAG, "video size: (" + mVideoWidth + "/" + mVideoHeight + ")");
                    mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
                    ///
                    mVideoView.onVideoSizeChanged(mVideoWidth, mVideoHeight, 1, 1);
                }
            }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
        new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG,"onCompletion");

            /*
            if(mPlaySpeed < 0){
                mPlaySpeed = 1;
                updateButtons(UPDATE_PLAYBACK_SPEED, 0, 0, mPlaySpeed);
                return;
            }
            */

            /*
            if(mPlaySpeed >= 2){
                mPlaySpeed = 1;
                updateButtons(UPDATE_PLAYBACK_SPEED, 0, 0, mPlaySpeed);
                return;
            }
            */

            stop();
            updateButtons(UPDATE_PLAYBACK_PROGRESS, 0, 0, 0);

            if(mLoopFile){
                //play();
                mVideoView.setVisibility(View.VISIBLE);  // call Play() in surfaceCreated()
                if(mCurSubtitleTrack >= 0){
                    Log.d(TAG,"onCompletion: select subtitle " + mCurSubtitleTrack);
                    mMediaPlayer.selectTrack(mCurSubtitleTrack);
                }
                if(mCurAudioTrack >= 0){
                    mMediaPlayer.selectTrack(mCurAudioTrack);
                    Log.d(TAG,"onCompletion: select audio " + mCurAudioTrack);
                }
            }
            /*
            if(mLoopFile){
                seekTo(0);
                start();
            }
            else
                stop();
            */
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener =
        new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            Log.d(TAG, "Error: " + framework_err + "," + impl_err);
            return true;
        }
    };

    private MediaPlayer.OnTimedTextListener mTimedTextListener =
        new MediaPlayer.OnTimedTextListener() {
        public void onTimedText(MediaPlayer mp, TimedText text){
            //Log.d(TAG,"onTimeText");
            if(mPlaySpeed == 1){
                if(text != null){
                    //Log.d(TAG,"text is " + text.getText());
                    mSubtitleTextView.setText(text.getText(), 0);
                }
                else
                    mSubtitleTextView.setText(null, 0);
            }
        }

    };


    class FileAccept implements FilenameFilter
    {
        String str =null;
        FileAccept(String s)
        {
            str = s;
        }
        public boolean accept(File dir,String name)
        {
            //Log.d(TAG,"str:" + str);
            //Log.d(TAG,"name:" + name);
            if(name.endsWith(".srt") && name.startsWith(str))
                return true;
            else
                return false;
        }
    }
    private void play() {
		if(mUrl != null) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnPreparedListener(mPreparedListener);
                mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
                mDuration = -1;
                mMediaPlayer.setOnCompletionListener(mCompletionListener);
                mMediaPlayer.setOnErrorListener(mErrorListener);
                mMediaPlayer.setOnTimedTextListener(mTimedTextListener);
                mMediaPlayer.setDataSource(mUrl);

                if(mVideoView == null)
                    Log.d(TAG, "mVideoView is null");
                if(mSurfaceHolder == null)
                    Log.d(TAG, "mSurfaceHolder is null");
                if(mSurfaceHolder.getSurface() == null)
                    Log.d(TAG, "surface is null");

                mMediaPlayer.setDisplay(mSurfaceHolder);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setScreenOnWhilePlaying(true);

                Log.w(TAG,"Url is " + mUrl);

                do{
                    if(mUrl.lastIndexOf('.') <= 0 || mUrl.lastIndexOf('.') >= mUrl.length()-1)
                        break;
                    if(mUrl.lastIndexOf('/') < 0 || mUrl.lastIndexOf('/') >= mUrl.length()-1)
                        break;

                    File f_url = new File(mUrl);
                    //Log.w(TAG,"abs path:" + f_url.getAbsolutePath() + " path:" + f_url.getPath() + " name:" + f_url.getName());
                    //Log.w(TAG,"parent:" + f_url.getParent());

                    File f_path = f_url.getParentFile();
                    String name = f_url.getName();
                    FileAccept filenameFilter = new FileAccept(name.substring(0, name.lastIndexOf('.')));
                    File list[] = f_path.listFiles(filenameFilter);
                    for(int i = 0; i < list.length; i++){
                        String srt_file = list[i].getAbsolutePath();
                        Log.w(TAG, "srt:" + srt_file);
                        mMediaPlayer.addTimedTextSource(srt_file, MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
                    }

                }while(false);
                mMediaPlayer.prepareAsync();
	        } catch (IOException ex) {
                Log.w(TAG, "Unable to open content: " + mUrl, ex);
                return;
            } catch (IllegalArgumentException ex) {
                Log.w(TAG, "Unable to open content: " + mUrl, ex);
                return;
            }
		}
	}

    public void start() {
        if(mMediaPlayer != null) {
            mMediaPlayer.start();
            mPlayState = PLAYER_PLAYING;
            mPlaySpeed = 1;
            if(mDuration < 0)
                mDuration = mMediaPlayer.getDuration();
        	updateButtons(UPDATE_PLAYBACK_STATE, mPlayState, 0, 0);
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    }

    private void pause() {
        if(mMediaPlayer != null) {
            mMediaPlayer.pause();
            mPlayState = PLAYER_PAUSED;
            updateButtons(UPDATE_PLAYBACK_STATE, mPlayState, 0, 0);
        }
    }

    private void stop() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayState = PLAYER_STOPPED;
            mPlaySpeed = 1;
            updateButtons(UPDATE_PLAYBACK_STATE, mPlayState, 0, 0);
            mSubtitleTextView.setText(null,0);
            mInfoView.setText(null,0);
            mVideoView.setVisibility(View.INVISIBLE);
        }
    }

    private void fastForward() {
        if(mMediaPlayer != null) {
            float newSpeed;
            int resultSpeed;
            if(mPlaySpeed < 0)
            newSpeed = 0.5f;
            else if(mPlaySpeed == 1)
            newSpeed = 0.5f;
            else if(mPlaySpeed == 0.5f)
            newSpeed = 1.5f;
            else if(mPlaySpeed == 1.5f)
            newSpeed = 2;
            else
            newSpeed = mPlaySpeed * 2;

            if(newSpeed > 16)
                newSpeed = 0.5f;

            int[] newScale = {(int)(newSpeed * 0x10000)};
            mMediaPlayer.setPlaySpeed(newScale);

            if(newScale[0] == newSpeed * 0x10000){
                mPlaySpeed = newSpeed;
                updateButtons(UPDATE_PLAYBACK_SPEED, 0, 0, mPlaySpeed);
            }

            //mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    }

	private void fastBackward() {
        Log.d(TAG,"fastBackward");

		if(mMediaPlayer != null) {
            float newSpeed;
            int resultSpeed;
			if(mPlaySpeed > -2)
                newSpeed = -2;
            else
                newSpeed = mPlaySpeed * 2;

            if(newSpeed < -16)
                newSpeed = -2;

            int[] newScale = {(int)(newSpeed * 0x10000)};
            mMediaPlayer.setPlaySpeed(newScale);

            if(newScale[0] == newSpeed * 0x10000){
                mPlaySpeed = newSpeed;
                updateButtons(UPDATE_PLAYBACK_SPEED, 0, 0, mPlaySpeed);
            }
            //mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}
	}

    private void setNormalSpeed() {
        if(mMediaPlayer != null) {
            mPlaySpeed = 1;
            int[] newScale = {(int)(mPlaySpeed * 0x10000)};
            mMediaPlayer.setPlaySpeed(newScale);
            updateButtons(UPDATE_PLAYBACK_SPEED, 0, 0, mPlaySpeed);
        }
    }

	private void seekTo(long ms) {
		if(mMediaPlayer != null) {
			mMediaPlayer.seekTo((int)ms);
		}
	}

    //------------------------------------------------------------------------------------
	// GUI messages
    //------------------------------------------------------------------------------------

	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		MenuItem item;

		super.onPrepareOptionsMenu(menu);

        Log.d(TAG,"==========onPrepareOptionsMenu ");

		boolean enabled = false;
		if(mPlaySpeed == 1 && (mPlayState == PLAYER_PLAYING || mPlayState == PLAYER_PAUSED))
			enabled = true;

        item = menu.findItem(SELECT_AUDIO);
        if(item != null)
            item.setEnabled(enabled);

        item = menu.findItem(SELECT_SUBTITLE);
        if(item != null)
            item.setEnabled(enabled);

        enabled = enabled && mCurSubtitleTrack >= 0;
        item = menu.findItem(CLOSE_SUBTITLE);
        if(item != null)
            item.setEnabled(enabled);

        return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG,"==========onCreateOptionsMenu ");
		super.onCreateOptionsMenu(menu);

        /*
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.operation, menu);
        */

        MenuItem item = menu.add(0, LOOP_FILE,	0, getString(R.string.LoopFile));
        item.setCheckable(true);
        item.setChecked(false);

        if(mMediaPlayer != null){
            MediaPlayer.TrackInfo[] ti ;
            try{
                ti = mMediaPlayer.getTrackInfo();
            }
            catch (Exception e)
            {
                Log.d(TAG, "Failed to get track info when creating menu");
                return false;
            }
            int totalCount = ti.length;

            Log.v(TAG,"total track count " + totalCount);
            for(int i = 0; i< totalCount ; i++){
                if(ti[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                    mAudioTrackCount++;
                else if(ti[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
                    mSubtitleTrackCount++;
            }
        }

        if(mAudioTrackCount >= 2){
    		menu.add(0, SELECT_AUDIO,	0, getString(R.string.SelectAudio));
        }
        if(mSubtitleTrackCount >= 1){
    		menu.add(0, SELECT_SUBTITLE,	0, getString(R.string.SelectSubtitle));
    		menu.add(0, CLOSE_SUBTITLE,	0, getString(R.string.CloseSubtitle));
        }

        return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = super.onOptionsItemSelected(item);

		int itemId = item.getItemId();

        if(itemId == LOOP_FILE){
            if (item.isChecked()) item.setChecked(false);
            else item.setChecked(true);
            mLoopFile = item.isChecked();
            //if(mMediaPlayer != null)
            //    mMediaPlayer.setLooping(mLoopFile);
        }
		else if(itemId == SELECT_AUDIO)
		{
            selectAudioDialog();
		}
		else if(itemId == SELECT_SUBTITLE)
		{
			selectSubtitleDialog();
		}
        else if(itemId == CLOSE_SUBTITLE)
        {
            if(mCurSubtitleTrack >= 0 && mMediaPlayer != null){
                mMediaPlayer.deselectTrack(mCurSubtitleTrack);
                mCurSubtitleTrack = -1;
                mCurSubtitleIndex = -1;
                mSubtitleTextView.setText(null,0);
            }
        }

		return ret;
	}


	/** Button messages
	 */
	private OnClickListener mOnBtnClicked = new OnClickListener() {
		public void onClick(View view) {
			if(view == mBtnFastBack)
			{
				fastBackward();
			}
			else if(view == mBtnPlayPause)
			{
				if(mPlayState == PLAYER_PLAYING){
    				pause();
                    if(mPlaySpeed != 1)
                        setNormalSpeed();
                }
                else if(mPlayState == PLAYER_PAUSED)
                    start();
				else if(mPlayState == PLAYER_STOPPED)
					//play();
					mVideoView.setVisibility(View.VISIBLE); // call play() in surfaceCreated()
			}
			else if(view == mBtnFastForward)
			{
			    if(mPlayState != PLAYER_PLAYING && mPlayState != PLAYER_PAUSED)
                    return;

			    if(mPlayState == PLAYER_PAUSED)
                    start();
				fastForward();
			}
		}
	};

    /** Seekbar messages
     */
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        //Log.d(TAG,"onProgressChanged, progress "  + progress + " fromUser " + fromUser);
        if (!fromUser) {
            // We're not interested in programmatically generated changes to
            // the progress bar's position.
            return;
        }
            if(mDuration == 0)
                    return;

        long newposition = (mDuration * progress) / 100L;
        seekTo( (int) newposition);
        updateButtons(UPDATE_PLAYBACK_PROGRESS, 0, progress * mDuration / 100, 0);

    }


    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.d(TAG,"onStartTrackingTouch");
        mDragging = true;
        mHandler.removeMessages(SHOW_PROGRESS);
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        mDragging = false;
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

	/** SurfaceHolder callbacks
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged: " + width + " x " + height);
		// fill with transparent color
//		mVideoView.setBackgroundColor(0x00000000);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		// fill with black color
        // mVideoView.setBackgroundColor(0xFF000000);

        //mSurfaceHolder  = holder;
		initPlay();
        play();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
        stop();
	}

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case SHOW_PROGRESS:
                    //Log.d(TAG,"handle SHOW_PROGRESS in mHandler");
                    if(mMediaPlayer == null)
                        break;

                    //Log.d(TAG,"handle message, mDragging " + mDragging + " isPlaying? "  + mMediaPlayer.isPlaying() );
                    if (!mDragging && mMediaPlayer.isPlaying()) {
                        //Log.d(TAG,"get position");
                        int curr = mMediaPlayer.getCurrentPosition();
                        //Log.d(TAG,"handleMessage: SHOW_PROGRESS: curr " + curr);

                        updateButtons(UPDATE_PLAYBACK_PROGRESS, 0, curr, 0);

                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 500);//1000 - (pos % 1000));
                    }
                    break;
            }
        }
    };

	private void selectAudioDialog() {

        String[] audioList;
        MediaPlayer.TrackInfo[] ti;
        int totalCount = 0;
        int audioCount = 0;

        if(mMediaPlayer == null)
            return;

        ti = mMediaPlayer.getTrackInfo();
        totalCount = ti.length;

        Log.v(TAG,"total track count " + totalCount);
        for(int i = 0; i< totalCount ; i++){
            if(ti[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                audioCount++;
        }

        audioList = new String[audioCount];
        audioCount = 0;
        for(int i = 0; i< totalCount ; i++){
            Log.v(TAG,"track " + i + " type is " + ti[i].getTrackType()+ " lan " + ti[i].getLanguage());
            if(ti[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO){
                audioList[audioCount] = ti[i].getLanguage();
                audioCount++;
            }
        }

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final	View dialogView = inflater.inflate(R.layout.selectaudio, null ,false);
		dialogView.setBackgroundResource(R.drawable.rounded_corners_view);

		final	PopupWindow pw = new PopupWindow(dialogView, 480, LayoutParams.WRAP_CONTENT, true);
		pw.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_pop));

		ListView list = (ListView) dialogView.findViewById(R.id.list);

		// add item index to chapter name
		String items[] = new String[audioList.length];
		int i;
		for(i=0; i<audioList.length; i++) {
			items[i] = Integer.toString(i+1) + ": "; // add index to title name
			if(audioList[i] != null){
                String fullName = map.get(audioList[i]);
                if(fullName != null)
                    items[i] += fullName;
                else
    				items[i] += "Audio track " + Integer.toString(i+1);
            }
			else
				items[i] += "Audio track " + Integer.toString(i+1);
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
			android.R.layout.simple_list_item_1,
			items);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> av, View v, int i, long l) {
				if(mMediaPlayer != null) {
					//String itemText = ((TextView)v).getText().toString();
					//String sub[] = itemText.split(":");
                    //Log.d(TAG,"====clicked item is " + itemText + " index " + i);
					//nativeExecuteCommand("at " + sub[0]);
					int index = -1;
                    MediaPlayer.TrackInfo[] ti = mMediaPlayer.getTrackInfo();
                    int totalCount = ti.length;
                    for(int j = 0; j< totalCount ; j++){
                        if(ti[j].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                            index++;
                        if(index == i){
                            //mMediaPlayer.selectTrack(j);

                            Parcel request = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            try {
                                request.writeInterfaceToken(IMEDIA_PLAYER);
                                request.writeInt(INVOKE_ID_SELECT_TRACK);
                                request.writeInt(j);
                                mMediaPlayer.invoke(request, reply);
                            } finally {
                                request.recycle();
                                reply.recycle();
                            }
                            //mInfoView.setText();
                            mCurAudioTrack = j;
                            mCurAudioIndex = i;
                            Log.d(TAG,"select audio track " + mCurAudioTrack);
                            break;
                        }
                    }

				}
				pw.dismiss();
			}
			});


		Button btnCancel = (Button)   dialogView.findViewById(R.id.BtnCancel);

		btnCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pw.dismiss();
				}
			});

		pw.showAtLocation(findViewById(R.id.ActiveWindow), Gravity.CENTER, 0, 0);
	}


	private void selectSubtitleDialog() {

        String[] subtitleList;
        MediaPlayer.TrackInfo[] ti;
        int totalCount = 0;
        int subtitleCount = 0;

        if(mMediaPlayer == null)
            return;

        ti = mMediaPlayer.getTrackInfo();
        totalCount = ti.length;

        Log.v(TAG,"total track count " + totalCount);
        for(int i = 0; i< totalCount ; i++){
            if(ti[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
                subtitleCount++;
        }

        subtitleList = new String[subtitleCount];
        subtitleCount = 0;
        for(int i = 0; i< totalCount ; i++){
            Log.v(TAG,"track " + i + " type is " + ti[i].getTrackType()+ " lan " + ti[i].getLanguage());
            if(ti[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT){
                subtitleList[subtitleCount] = ti[i].getLanguage();
                subtitleCount++;
            }
        }

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final	View dialogView = inflater.inflate(R.layout.selectsubtitle, null ,false);
		dialogView.setBackgroundResource(R.drawable.rounded_corners_view);

		final	PopupWindow pw = new PopupWindow(dialogView, 480, LayoutParams.WRAP_CONTENT, true);
		pw.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_pop));

		ListView list = (ListView) dialogView.findViewById(R.id.list);

		// add item index to chapter name
		String items[] = new String[subtitleList.length];
		int i;
		for(i=0; i<subtitleList.length; i++) {
			items[i] = Integer.toString(i+1) + ": "; // add index to title name
			if(subtitleList[i] != null){
                String fullName = map.get(subtitleList[i]);
                if(fullName != null)
                    items[i] += fullName;
                else
    				items[i] += "Subtitle track " + Integer.toString(i+1);
            }
			else
				items[i] += "Subtitle track " + Integer.toString(i+1);
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
			android.R.layout.simple_list_item_1,
			items);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> av, View v, int i, long l) {
				if(mMediaPlayer != null) {
					int index = -1;
                    MediaPlayer.TrackInfo[] ti = mMediaPlayer.getTrackInfo();
                    int totalCount = ti.length;
                    for(int j = 0; j< totalCount ; j++){
                        if(ti[j].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
                            index++;
                        if(index == i){
                            mMediaPlayer.selectTrack(j);
                            /*
                            Parcel request = Parcel.obtain();
                            Parcel reply = Parcel.obtain();
                            try {
                                request.writeInterfaceToken(IMEDIA_PLAYER);
                                request.writeInt(INVOKE_ID_SELECT_TRACK);
                                request.writeInt(j);
                                mMediaPlayer.invoke(request, reply);
                            } finally {
                                request.recycle();
                                reply.recycle();
                            }
                            */
                            //mInfoView.setText();
                            mCurSubtitleTrack = j;
                            mCurSubtitleIndex = i;
                            break;
                        }
                    }

				}
				pw.dismiss();
			}
			});


		Button btnCancel = (Button)   dialogView.findViewById(R.id.BtnCancel);

		btnCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pw.dismiss();
				}
			});

		pw.showAtLocation(findViewById(R.id.ActiveWindow), Gravity.CENTER, 0, 0);
	}


	private void pauseMetadataShow() {
		//mErrSign.pause();
		mInfoView.pause();
        mSubtitleTextView.pause();
	}

	private void resumeMetadataShow() {
		//mErrSign.resume();
		mInfoView.resume();
        mSubtitleTextView.resume();
	}

	private Map<String, String> map = new HashMap<String, String>();

	private void initLocaleTable() {
		map.put("alb", "Albanian");
		map.put("sqi", "Albanian");
		map.put("sq" , "Albanian");
		map.put("ara", "Arabic");
		map.put("ar" , "Arabic");
		map.put("arm", "Armenian");
		map.put("hye", "Armenian");
		map.put("hy" , "Armenian");
		map.put("art", "Artificial languages");
		map.put("ast", "Asturian");
		map.put("aus", "Australian languages");
		map.put("aze", "Azerbaijani");
		map.put("az" , "Azerbaijani");
		map.put("ast", "Bable");
		map.put("bat", "Baltic languages");
		map.put("bam", "Bambara");
		map.put("bm" , "Bambara");
		map.put("chi", "Chinese");
		map.put("zho", "Chinese");
		map.put("zh" , "Chinese");
		map.put("zha", "Chuang");
		map.put("za" , "Chuang");
		map.put("mus", "Creek");
    	map.put("cze", "Czech");
		map.put("ces", "Czech");
		map.put("cs" , "Czech");
		map.put("dan", "Danish");
		map.put("da" , "Danish");
		map.put("dut", "Dutch");
		map.put("nld", "Dutch");
		map.put("nl" , "Dutch");
		map.put("eng", "English");
		map.put("en" , "English");
		map.put("est", "Estonian");
		map.put("et" , "Estonian");
		map.put("fan", "Fang");
		map.put("fat", "Fanti");
		map.put("fin", "Finnish");
		map.put("fi" , "Finnish");
		map.put("fre", "French");
		map.put("fra", "French");
		map.put("fr" , "French");
		map.put("geo", "Georgian");
		map.put("kat", "Georgian");
		map.put("ka" , "Georgian");
		map.put("ger", "German");
		map.put("deu", "German");
		map.put("de" , "German");
		map.put("hun", "Hungarian");
		map.put("hu" , "Hungarian");
		map.put("ice", "Icelandic");
		map.put("isl", "Icelandic");
		map.put("is" , "Icelandic");
		map.put("ind", "Indonesian");
		map.put("in" , "Indonesian");
		map.put("ira", "Iranian languages");
		map.put("gle", "Irish");
		map.put("ga" , "Irish");
		map.put("ita", "Italian");
		map.put("it" , "Italian");
		map.put("jpn", "Japanese");
		map.put("ja" , "Japanese");
		map.put("kon", "Kongo");
		map.put("kg" , "Kongo");
		map.put("kor", "Korean");
		map.put("ko" , "Korean");
		map.put("kur", "Kurdish");
		map.put("ku" , "Kurdish");
		map.put("lao", "Lao");
		map.put("lo" , "Lao");
		map.put("lat", "Latin");
		map.put("la" , "Latin");
		map.put("per", "Persian");
		map.put("fas", "Persian");
		map.put("fa" , "Persian");
		map.put("phi", "Philippine languages");
		map.put("pol", "Polish");
		map.put("pl" , "Polish");
		map.put("por", "Portuguese");
		map.put("pt" , "Portugueses");
		map.put("rum", "Romanian");
		map.put("ron", "Romanian");
		map.put("ro" , "Romanian");
		map.put("rus", "Russian");
		map.put("ru" , "Russian");
		map.put("sco", "sco");
		map.put("srp", "Serbian");
		map.put("sr" , "Serbian");
		map.put("spa", "Spanish");
		map.put("es" , "Spanish");
		map.put("swe", "Swedish");
		map.put("sv" , "Swedish");
		map.put("tha", "Thai");
		map.put("th" , "Thai");
		map.put("tur", "Turkish");
		map.put("tr" , "Turkish");
		map.put("ukr", "Ukrainian");
		map.put("uk" , "Ukrainian");
		map.put("vie", "Vietnamese");
		map.put("vi" , "Vietnamese");
		map.put("nor", "Norwegian");
		map.put("no" , "Norwegian");
		map.put("grc", "Greek");
		map.put("gre", "Greek");
		map.put("ell", "Greek");
		map.put("el" , "Greek");

	}

	private String localeToLangName(String locale) {
		String name = null;

		if(locale != null)
			name = map.get(locale);

		if(name == null)
			name = "Unspecified language";

		return name;
	}


}

