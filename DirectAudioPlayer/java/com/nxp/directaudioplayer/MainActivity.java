/*
 * Copyright (C) 2018 NXP Semiconductor, Inc.
 */
package com.nxp.directaudioplayer;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class MainActivity extends Activity {
    int positionTime=0;
    ListView mFileList;
    TextView mSelectedFfileNameText;
    TextView mTime;
    String mSelectedFileName;
    File mRootdDirectory;
    ThreadPlay threadPlay = new ThreadPlay();
    private static String TAG = "AudioTrack";
    public final static int PERMISSION_REQUESTCODE = 1;
    private static final int AUTO_UPDATE_TIMER = 1000;
    List<String> permissionLists = new ArrayList<>();
    int cnt = 0;
    boolean avoid_RedundantClickCrash = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();

    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            initView();
        }catch (Exception e){
            Log.e(TAG,e.toString());
        }
    }

    private void permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionLists.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionLists.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionLists.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionLists.toArray(new String[permissionLists.size()]), PERMISSION_REQUESTCODE);
        }else{
            Log.i(TAG,"permission not allowed");
        }
    }

    View.OnClickListener playclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int playstate =0;
            if(threadPlay.mTrack!=null){
                playstate = threadPlay.mTrack.getPlayState();
            }
            if(playstate==0){
                threadPlay.start();
                avoid_RedundantClickCrash = true;
            }
            if(playstate== AudioTrack.PLAYSTATE_PAUSED){
                threadPlay.mTrack.play();
            }
            mHandle.postDelayed(mAutoTimerRunnable,AUTO_UPDATE_TIMER);
        }
    };

    private Runnable mAutoTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if(threadPlay!=null){
                if(threadPlay.mTrack!=null){
                    synchronized (threadPlay.mTrack){
                        if(threadPlay.mTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING){
                            positionTime = threadPlay.mTrack.getPlaybackHeadPosition() / threadPlay.rate;
                        }
                    }
                }
            }
            Message msg = new Message();
            msg.arg1 = positionTime;
            mHandle.sendMessage(msg);
            mHandle.postDelayed(this,AUTO_UPDATE_TIMER);
        }
    };

    private Handler mHandle = new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            mTime.setText("Have played "+new String(String.valueOf(msg.arg1))+" seconds");
        }
    };
    View.OnClickListener pauselick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            threadPlay.mTrack.pause();
        }
    };

    private void initView() {
        mFileList = (ListView) findViewById(R.id.listSongs);
        Button mPlayButton = (Button) findViewById(R.id.Play);
        Button mPauseButton = findViewById(R.id.Pause);
        mSelectedFfileNameText = (TextView) findViewById(R.id.Display_SelectedFile);
        mTime = findViewById(R.id.Time);
        mPlayButton.setOnClickListener(playclick);
        mPauseButton.setOnClickListener(pauselick);
        mRootdDirectory = Environment.getExternalStorageDirectory();
        mSelectedFfileNameText.setText("");
        mTime.setText("");
        listFiles();
    }

    class Filter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".wav"));
        }
    }

    private void listFiles() {
        Filter myfilter = new Filter();
        String[] children = mRootdDirectory.list(myfilter);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.file_row, children);
        mFileList.setAdapter(spinnerArrayAdapter);
        mFileList.setFocusable(true);
        mFileList.setFocusableInTouchMode(true);
        mFileList.setItemsCanFocus(true);
        mFileList.setOnItemClickListener(listClick);
    }

    AdapterView.OnItemClickListener listClick = new AdapterView.OnItemClickListener() {

        public void onItemClick(AdapterView adapterView, View view,
                                int arg2, long arg3) {
            long selectedPosition = arg3;
            mSelectedFileName = (String) mFileList.getItemAtPosition((int) selectedPosition);
            mSelectedFfileNameText.setText("File selected:  "+mSelectedFileName);
            while (avoid_RedundantClickCrash){
                if(cnt>0){
                    threadPlay.isPlaying = false;
                    synchronized (threadPlay.mTrack){
                        threadPlay.mTrack.stop();
                        threadPlay.mTrack.release();
                    }
                    threadPlay = new ThreadPlay();
                }
                cnt++;
                avoid_RedundantClickCrash = false;
            }
            threadPlay.setFile(mSelectedFileName);
        }
    };

    class ThreadPlay extends Thread {
        private static final String TAG = "PLAYMUSIC";
        private String mFileName;
        int rate = 48000;
        byte bits = 16;
        byte chans = 2;
        AudioTrack mTrack = null;
        FileInputStream minputStream = null;
        File mTempFile;
        int read = 0;
        boolean isPlaying = true;
        byte[] mediaBuffer;
        int minBufSize;
        public void setFile(String filename) {
            this.mFileName = filename;
        }
        public void run() {
            try {
                mTempFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/" + mFileName);
            } catch (Exception ex) {
                Log.e(TAG,"Fail to find the file");
            }
            try {
                minputStream = new FileInputStream(mTempFile);
            } catch (Exception ex) {
                Log.e(TAG,"Fail to transform to inputstream");
            }
            paserWAVheader();
            int AudioFmtChn;
            int AudioFmtBits;
            if (chans == 1)
                AudioFmtChn = AudioFormat.CHANNEL_IN_MONO;
            else if (chans == 2)
                AudioFmtChn = AudioFormat.CHANNEL_IN_STEREO;
            else if (chans == 6)
                AudioFmtChn = AudioFormat.CHANNEL_OUT_5POINT1;
            else {
                Log.e(TAG, "unsupported channel num " + chans + ", treat as stereo");
                return;
            }
            if (bits == 8)
                AudioFmtBits = AudioFormat.ENCODING_PCM_8BIT;
            else if (bits == 16)
                AudioFmtBits = AudioFormat.ENCODING_PCM_16BIT;
            else {
                Log.e(TAG, "unsupported bits " + bits + ", treat as 16 bits");
                return;
            }
            minBufSize = AudioTrack.getMinBufferSize(rate, AudioFmtChn, AudioFmtBits);
            minBufSize*= 100;
            mediaBuffer = new byte[minBufSize];
            mTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setFlags(AudioAttributes.FLAG_HW_AV_SYNC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                    new AudioFormat.Builder()
                            .setSampleRate(rate)
                            .setEncoding(AudioFmtBits)
                            .setChannelMask(AudioFmtChn).build(),
                    minBufSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );
            mTrack.play();
            while(mTrack!=null) {
                while (isPlaying && mTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        read = minputStream.read(mediaBuffer);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    mTrack.write(mediaBuffer, 0, read);
                    if(read<0) break;
                    if(isPlaying ==false) break;
                }
            }
            Log.i(TAG, "Done playing");
            try {
                minputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mTrack.stop();
            Log.i(TAG, "mtrack stop in play thread");
            mTrack.release();
            Log.i(TAG,"mtrack release in play thread");
        }

        public void paserWAVheader() {
            byte[] head = new byte[44];
            try {
                read = minputStream.read(head);
                Log.i(TAG,"read is "+read);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (!((head[0] == 'R') && (head[1] == 'I') && (head[2] == 'F') && (head[3] == 'F'))) {
                Log.i(TAG, "error RIFF");
                return;
            }

            if (!((head[8] == 'W') && (head[9] == 'A') && (head[10] == 'V') && (head[11] == 'E'))) {
                Log.i(TAG, "error WAVE");
                return;
            }
            rate = (int) (head[24] & 0x0FF) +
                    ((int) (head[24 + 1] & 0x0FF) << 8) +
                    ((int) (head[24 + 2] & 0x0FF) << 16) +
                    ((int) (head[24 + 3] & 0x0FF) << 24);
            bits = head[34];
            chans = head[22];
            Log.i(TAG, "rate " + rate + ", bits " + bits + ", chans " + chans);
        }
    }
}