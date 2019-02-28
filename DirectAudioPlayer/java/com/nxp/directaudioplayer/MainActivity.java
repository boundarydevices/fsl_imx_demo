/*
 * Copyright 2018 NXP.
 */
package com.nxp.directaudioplayer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTimestamp;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class MainActivity extends Activity {
    int mLPA = 0;
    int positionTime=0;
    ListView mFileList;
    TextView mSelectedFfileNameText;
    TextView mTime;
    String mSelectedFileName;
    File mRootdDirectory;
    ThreadPlay threadPlay = new ThreadPlay();
    private static String TAG = "AudioTrack";
    public final static int PERMISSION_REQUESTCODE = 1;
    public final static String LPA_ENABLE_PROPERTY = "vendor.audio.lpa.enable";
    private static final int AUTO_UPDATE_TIMER = 1000;
    // For 48KHz audio, this buffer can hold 30s data
    private static final int BUFFER_SIZE = 5760000;
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
            checkLPAMode();
        }catch (Exception e){
            Log.e(TAG,e.toString());
        }
    }

    private void checkLPAMode() {
        Process mProcess = null;
        try {
            mProcess = Runtime.getRuntime().exec("getprop " + LPA_ENABLE_PROPERTY);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
        try {
            mLPA = Integer.parseInt(in.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "lpa_enable: " + mLPA);
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
            Log.i(TAG, "play click");
            int playstate =0;
            if(threadPlay.mTrack!=null){
                playstate = threadPlay.mTrack.getPlayState();
            }
            if(playstate==0){
                threadPlay.start();
                avoid_RedundantClickCrash = true;
                mHandle.postDelayed(mAutoTimerRunnable,AUTO_UPDATE_TIMER);
            }
            if(playstate== AudioTrack.PLAYSTATE_PAUSED){
                threadPlay.mTrack.play();
            }
        }
    };

    private Runnable mAutoTimerRunnable = new Runnable() {
        @Override
        public void run() {
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
            Log.i(TAG, "pause click");
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
        float[] mediaBufferFloat;
        int minBufSize;
        int frameSize;

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
                AudioFmtChn = AudioFormat.CHANNEL_OUT_MONO;
            else if (chans == 2)
                AudioFmtChn = AudioFormat.CHANNEL_OUT_STEREO;
            else if (chans == 4)
                AudioFmtChn = AudioFormat.CHANNEL_OUT_QUAD;
            else if (chans == 6)
                AudioFmtChn = AudioFormat.CHANNEL_OUT_5POINT1;
            else if (chans == 8)
                AudioFmtChn = AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;
            else {
                Log.e(TAG, "unsupported channel num " + chans + ", treat as stereo");
                return;
            }
            if (bits == 8)
                AudioFmtBits = AudioFormat.ENCODING_PCM_8BIT;
            else if (bits == 16)
                AudioFmtBits = AudioFormat.ENCODING_PCM_16BIT;
            else if (bits == 24)
                AudioFmtBits = AudioFormat.ENCODING_PCM_FLOAT;
            else if (bits == 32)
                AudioFmtBits = AudioFormat.ENCODING_PCM_FLOAT;
            else {
                Log.e(TAG, "unsupported bits " + bits + ", treat as 16 bits");
                return;
            }

            frameSize = chans * bits/8;

            AudioManager mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if (bits == 16)
                mAudioManager.setParameters("pcm_bit=16");
            else if (bits == 24)
                mAudioManager.setParameters("pcm_bit=24");
            else if (bits == 32)
                mAudioManager.setParameters("pcm_bit=32");

            if (mLPA == 1)
                minBufSize = BUFFER_SIZE;
            else
                minBufSize = AudioTrack.getMinBufferSize(rate, AudioFmtChn, AudioFmtBits);
            Log.e(TAG, "Buffer size: " + minBufSize );

            mediaBuffer = new byte[minBufSize];
            // For stream rate <= 192000, channels <=2, it can obviously be attached to a mixed output,
            // AudioPolicyManager will use mixer thread(primary thread) to play them.
            // In App layer, we can use FLAG_HW_AV_SYNC to explicitly request AudioPolicyManager
            // to use DirectOutput thread.
            if (rate <= 192000 && chans <= 2) {
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
            } else {
                mTrack = new AudioTrack(
                        new AudioAttributes.Builder()

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
            }

            mTrack.play();
            long totalFeedSize = 0;
            Log.i(TAG, "begin feed data, buffer size " + minBufSize);
            while (isPlaying) {
                int state = mTrack.getPlayState();

                if(state ==  AudioTrack.PLAYSTATE_STOPPED) {
                    Log.i(TAG, "PLAYSTATE_STOPPED");
                    break;
                }

                if(state == AudioTrack.PLAYSTATE_PAUSED) {
                    Log.v(TAG, "PLAYSTATE_PAUSED");
                    try {
                      Thread.sleep(10);
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                    continue;
                }

                try {
                    read = minputStream.read(mediaBuffer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if(read<0) { Log.i(TAG, "break since read meet EOF, ret " + read); break; }

                int ret = 0;
                int written = 0;
                int toWrite = read;

                // In pause stae, mTrack.write may exit with written < read.
                while(written < read) {
                    if (bits == 24 || bits == 32) {
                        mediaBufferFloat = byteArrayToFloatArray(mediaBuffer);
                        ret = mTrack.write(mediaBufferFloat, written/4, toWrite/4, AudioTrack.WRITE_BLOCKING);
                        ret = ret*4; // convert float to byte length
                    } else {
                        ret = mTrack.write(mediaBuffer, written, toWrite);
                    }

                    if(ret < 0) {
                        Log.i(TAG, "write " + toWrite + " bytes failed, ret " + ret + ", break");
                        break;
                    }

                    written += ret;
                    if(written < read) {
                       toWrite = read - written;

                       try {
                           Thread.sleep(10);
                       } catch (InterruptedException e) {
                          e.printStackTrace();
                       }
                    }
                } // write cycle

                totalFeedSize += written;
                positionTime = (int)totalFeedSize / frameSize / rate;
            } // while (isPlaying)

            if(mLPA == 1) {
                AudioTimestamp timestamp = new AudioTimestamp();
                mTrack.getTimestamp(timestamp);

                long playedFrame = timestamp.framePosition;
                long frameSize = chans*bits/8;
                long totalFrame = totalFeedSize/frameSize;
                long restFrame = totalFrame - playedFrame;
                long restTimeMs = restFrame*1000/rate;

                Log.i(TAG, "Done feeding data, total frames " + totalFrame + ", played frames " + playedFrame +
                      ", rest frames " + restFrame + ", rest ms " + restTimeMs + ", frame size " + frameSize);

                try {
                  Thread.sleep(restTimeMs);
                } catch (InterruptedException e) {
                  e.printStackTrace();
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

        public float[] byteArrayToFloatArray(byte[] bytes) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();

            float[] array = new float[floatBuffer.remaining()];
            floatBuffer.get(array);
            return array;
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
