/*
 * Copyright (C) 2013 Freescale Semiconductor, Inc.
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

package com.freescale.wfdsink;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.GestureDetector;
import android.widget.VideoView;
import android.view.MotionEvent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRouter.RouteInfo;
import android.media.MediaRouter;
import android.app.Presentation;
import java.io.File;
import java.io.FilenameFilter;
import android.os.Bundle;
import android.content.DialogInterface;
import android.content.IntentFilter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;
import android.net.Uri;
import android.content.Intent;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.view.Display;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import com.fsl.wfd.WfdSink;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.WindowManager;
import android.view.Display;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.net.wifi.p2p.WifiP2pDevice;
import android.content.Intent;
import android.os.Parcelable;
import android.view.View.OnClickListener;
public class WfdSinkActivity extends Activity implements SurfaceHolder.Callback,OnClickListener
{
    private static final String TAG = "WfdSinkActivity";
    private static final int UPDATE_GRID_VIEW = 0x10;
    private static final int UPDATE_BUTTON_SHOW = 0x11;
    private static final int UPDATE_SURFACE = 0x12;
    private static final int START_PLAY = 0x13;
    private static final int STOP_PLAY = 0x14;
    private static final int DO_CONNECTED = 0x15;
    private static final int START_SEARCH = 0x16;
    private static final int DELAY = 1000;
    private static final int PERIOD = 3000;
    private WfdSink mWfdSink;
    private SurfaceHolder mSurfaceHolder = null;
    private boolean mWaitingForSurface = false;
    private boolean mStarted = false;
    private boolean mConnected = false;

    private ImageButton mImageButton;
    private ArrayList<String> mSourcePeers = new ArrayList<String>();
    private GridView mGridView;
    private SurfaceView mSurfaceView;
    private PictureAdapter mPictureAdapter;
    DisplayMetrics mDisplayMetrics;
    private boolean mButtonShow = false;
    private Timer mTimer;
    private String mThisName;
    private TextView currentdevice;
    private TextView status;
    private LinearLayout layout;    
    private final BroadcastReceiver mWifiP2pReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WfdSink.WFD_DEVICE_LIST_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice[] devices;
                Parcelable[] devs;

                devs = intent.getParcelableArrayExtra(WfdSink.EXTRA_DEVICE_LIST);
                if (devs == null || devs.length == 0) {
                    return;
                }

                devices = new WifiP2pDevice[devs.length];
                for (int i=0; i<devs.length; i++) {
                    devices[i] = (WifiP2pDevice)devs[i];
                }

                mSourcePeers.clear();
                for (WifiP2pDevice device : devices) {
                    mSourcePeers.add(device.deviceName);
                }

                mPictureAdapter.setSourcePeers(mSourcePeers);
                mHandler.sendEmptyMessage(UPDATE_GRID_VIEW);
                mGridView.postInvalidate();
            }
            if (WfdSink.WFD_DEVICE_CONNECTED_ACTION.equals(action)) {
                Message msg = mHandler.obtainMessage(DO_CONNECTED);
                boolean connected = intent.getBooleanExtra(WfdSink.EXTRA_CONNECTION_CHANGED, false);
                msg.arg1 = (connected == true) ? 1 : 0;
                mHandler.sendMessage(msg);
            }

            if (WfdSink.WFD_THIS_DEVICE_UPDATE_ACTION.equals(action)) {
                currentdevice = (TextView)findViewById(R.id.currentdevice);
                if (mThisName != mWfdSink.getDeviceName())
                    mThisName = mWfdSink.getDeviceName();
                currentdevice.setText("SSID:" + mWfdSink.getDeviceName());
            }

        }
    };


    private View sink_main;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WfdSink.WFD_DEVICE_LIST_CHANGED_ACTION);
        intentFilter.addAction(WfdSink.WFD_DEVICE_CONNECTED_ACTION);
        intentFilter.addAction(WfdSink.WFD_THIS_DEVICE_UPDATE_ACTION);
        registerReceiver(mWifiP2pReceiver, intentFilter);
        sink_main = getLayoutInflater().from(this).inflate(R.layout.sink_main, null);  
        sink_main.setOnClickListener(this);

        mWfdSink = new WfdSink(this);
        setContentView(sink_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.sink_preview);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        mGridView = (GridView)findViewById(R.id.gridview);
        mImageButton = (ImageButton)findViewById(R.id.sink_player);

        status = (TextView)findViewById(R.id.status);
        layout = (LinearLayout)findViewById(R.id.linearLayout1);
        if(manager.isWifiEnabled()){

        }else{
            wifiDialog();
        }
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                switch(arg0.getId()) {
                    case R.id.sink_player:
                        startSearch();
                        break;
                    default:
                        break;
                }
            }
        });

        mPictureAdapter = new PictureAdapter(this);
        mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

        mSurfaceView.setVisibility(View.INVISIBLE);
        mGridView.setVisibility(View.VISIBLE);
        mGridView.setAdapter(mPictureAdapter);

        startSearch();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WfdSink.WFD_DEVICE_LIST_CHANGED_ACTION);
        intentFilter.addAction(WfdSink.WFD_DEVICE_CONNECTED_ACTION);
        intentFilter.addAction(WfdSink.WFD_THIS_DEVICE_UPDATE_ACTION);
        registerReceiver(mWifiP2pReceiver, intentFilter);
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        mWfdSink = new WfdSink(this);
        if(manager.isWifiEnabled()){

        }else{
            wifiDialog();
        }
        startSearch();
    }

    @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if(keyCode == KeyEvent.KEYCODE_BACK)
            {
                this.exitDialog();
            }
            return super.onKeyDown(keyCode, event);
        }

    private void wifiDialog() {
        Dialog dialog = new AlertDialog.Builder(WfdSinkActivity.this)
            .setTitle("WI-FI hasn't open ! ").setMessage("Are you sure to open now? If you cancel then will exit!").setIcon(R.drawable.ic_hdmi)
            .setPositiveButton("confirm", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                    }
                    }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(-1);
                        }
                        }).create();
        dialog.show();
    }

    private void exitDialog() {
        Dialog dialog = new AlertDialog.Builder(WfdSinkActivity.this)
            .setTitle("Program exit ?")
            .setMessage("Are you sure to exit the program ?")
            .setIcon(R.drawable.ic_hdmi)
            .setPositiveButton("confirm", new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    WfdSinkActivity.this.finish();

                    }

                    }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                        }
                        }).create();
        dialog.show();

    }

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_GRID_VIEW:
                    mGridView.setAdapter(mPictureAdapter);
                    break;

                case UPDATE_SURFACE:
                    SurfaceHolder holder = (SurfaceHolder)msg.obj;
                    handleUpdateSurface(holder);
                    break;

                case START_PLAY:                	
                    handleStartPlay();
                    break;

                case STOP_PLAY:
                    handleStopPlay();
                    break;

                case DO_CONNECTED:
                    status.setText("Status:Connected");
                    boolean connected = (msg.arg1 == 1);
                    handleConnected(connected);
                    sink_main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    mTimer.cancel();
                    break;
                case START_SEARCH:
                    mWfdSink.startSearch();
                    break;
            }
        }
    };

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

        Log.i(TAG, "surfaceChanged. w=" + w + ". h=" + h);
        Message msg = mHandler.obtainMessage(UPDATE_SURFACE, holder);
        mHandler.sendMessage(msg);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated.");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed.");
        mSurfaceHolder = null;
    }

    private void handleUpdateSurface(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        if (mWaitingForSurface) {
            mWaitingForSurface = false;
            mWfdSink.startRtsp(mSurfaceHolder.getSurface());
            mStarted = true;
        }
    }

    private void handleStartPlay() {
        if (mSurfaceHolder == null) {
            mWaitingForSurface = true;
            return;
        }
        mWfdSink.startRtsp(mSurfaceHolder.getSurface());
        mStarted = true;
    }

    private void handleStopPlay() {
        if (!mStarted) {
            return;
        }
        mWfdSink.stopRtsp();

        mSourcePeers.clear();
        mPictureAdapter.setSourcePeers(mSourcePeers);
        mGridView.setAdapter(mPictureAdapter);
        mGridView.postInvalidate();
        layout.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.INVISIBLE);
        mGridView.setVisibility(View.VISIBLE);
        mStarted = false;
    }

    private void handleConnected(boolean connected) {
        mConnected = connected;
        if (mConnected) {
            stopSearch();
        	layout.setVisibility(View.INVISIBLE);
            mSurfaceView.setVisibility(View.VISIBLE);
            mGridView.setVisibility(View.GONE);
            startPlayer();
        }
        else {
            stopPlayer();
            startSearch();
        }
    }

    private void startSearch() {
        status.setText("Status:Searching for peers");
        mWfdSink.startSearch();
        //mWfdSink.setDeviceName("Android_me");
    }

    private void stopSearch() {
        mWfdSink.stopSearch();
    }

    private void disconnectPeer() {
        mWfdSink.disconnect();
    }

    private void startPlayer() {
        Message msg = mHandler.obtainMessage(START_PLAY);
        mHandler.sendMessage(msg);
    }

    private void stopPlayer() {
        status.setText("Status:Disconnected!");
        Message msg = mHandler.obtainMessage(STOP_PLAY);
        mHandler.sendMessage(msg);
    }


    public void onStart() {
        super.onStart();
        mGridView.setAdapter(mPictureAdapter);
    }

    public  void onResume() {
        super.onResume();
        TimerTask task = new TimerTask(){  
            public void run() {     
                mHandler.sendEmptyMessage(START_SEARCH);    
            }  
        };
        mTimer = new Timer(true);
        mTimer.schedule(task,DELAY, PERIOD);
    }

    public void onStop() {
        super.onStop();
        unregisterReceiver(mWifiP2pReceiver);
        if (mStarted) {
            handleStopPlay();
        }

        if (mConnected) {
            disconnectPeer();
            mConnected = false;
        }
        stopSearch();
        mWfdSink.dispose();
        mWfdSink = null;

    }

    public void onDestroy() {
        super.onDestroy();
    }

    public class ViewHolder {
        public TextView mTitle;
        public ImageView mImage;
    }

    public class PictureAdapter extends BaseAdapter {
        private List<String> mPeers;
        private LayoutInflater mInflater;

        public PictureAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public void setSourcePeers(List<String> sourcePeers) {
            Log.w(TAG, "setSourcePeers");
            mPeers = sourcePeers;
        }

        @Override
        public int getCount() {
            if (mPeers != null) {
                return mPeers.size();
            }
            else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            Log.w(TAG, "getItem:" + mPeers.get(position));
            return mPeers.get(position);
        }

        @Override
        public long getItemId(int position) {
                return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.sink_pic, null);
                viewHolder = new ViewHolder();
                viewHolder.mTitle = (TextView)convertView.findViewById(R.id.pic_title);
                viewHolder.mImage = (ImageView)convertView.findViewById(R.id.pic_image);
                convertView.setTag(viewHolder);
            }
            else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            viewHolder.mTitle.setText((String)getItem(position));
            viewHolder.mImage.setImageResource(R.drawable.ic_hdmi);

            LayoutParams para;
            para = viewHolder.mImage.getLayoutParams();
            para.width = mDisplayMetrics.widthPixels/4;
            para.height = mDisplayMetrics.heightPixels/3;
            viewHolder.mImage.setLayoutParams(para);
            return convertView;
        }
    }
        @Override
        public void onClick(View v) {

            int i = sink_main.getSystemUiVisibility();
            if (i == View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
                sink_main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else if (i == View.SYSTEM_UI_FLAG_VISIBLE){
                sink_main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else if (i == View.SYSTEM_UI_FLAG_LOW_PROFILE) {
                sink_main.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
}

