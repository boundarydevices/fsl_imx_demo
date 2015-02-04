/*
 * Copyright (C) 2015 Freescale Semiconductor, Inc.
 */
package com.freescale.a2dpsinkapp;

import android.app.Activity;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.os.AsyncTask;
import android.widget.Button;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAvrcpController;
import android.content.BroadcastReceiver;

import java.util.HashMap;
import java.util.List;

/*Created by b50027 Jan.19 2015 */

public class MainActivity extends Activity {

    boolean DBG = true;

    String TAG = "A2dpSinkApp";
    AudioRecord mRecord = null;
    AudioTrack mTrack = null;
    boolean isPlaying = false;
    A2dpSinkWorkTask mPlayTask;
    Button btn_start;
    Button btn_stop;
    ImageButton btn_enable;
    BluetoothA2dpSink mA2dpSinkService;
    BluetoothAvrcpController mAvrcpControllerService;
    BroadcastReceiverA2dpSink mBroadcastReceiverA2dpSink;
    int a2dpState = BluetoothA2dpSink.STATE_DISCONNECTED;
    BluetoothDevice a2dpRemoteDevice = null;
    String connectDeviceInfo = null;
    HashMap<Integer,Integer> button_release_icon = new HashMap<Integer,Integer>();
    HashMap<Integer,Integer> button_press_icon = new HashMap<Integer, Integer>();
    HashMap<Integer,Integer> button_avrcp_cmd = new HashMap<Integer, Integer>();
    Handler avrcpHandler = new Handler();
    static int AVRCP_CMD_PREVIOUS = 0x4c;
    static int AVRCP_CMD_NEXT = 0x4b;
    static int AVRCP_CMD_PLAY  = 0x44;
    static int AVRCP_CMD_PAUSE = 0x46;
    static int AVRCP_BTN_PRESS = 0;
    static int AVRCP_BTN_RELEASE = 1;


    class EnableButtonlistener implements OnClickListener, OnTouchListener {
        public void onClick(View v) {
            if (DBG) Log.d(TAG, "power Button clicked");
        }

        public boolean onTouch(View v, MotionEvent event) {

            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                if (isPlaying) {
                    btn_enable.setBackgroundResource(R.drawable.power_on_pressed);
                } else
                    btn_enable.setBackgroundResource(R.drawable.power_off_pressed);
            } else if (action == MotionEvent.ACTION_UP) {
                if (a2dpState == BluetoothA2dpSink.STATE_DISCONNECTED) {
                    isPlaying = false;
                    Log.i(TAG, "Stop!!!");
                    uiSetA2dpConnectState(a2dpState == BluetoothA2dpSink.STATE_CONNECTED,
                            connectDeviceInfo);
                    btn_enable.setBackgroundResource(R.drawable.power_off);
                }
                if (a2dpState == BluetoothA2dpSink.STATE_CONNECTED) {
                    if (isPlaying) {
                        isPlaying = false;
                        Log.i(TAG, "Stop!!!");
                        uiSetA2dpConnectState(a2dpState == BluetoothA2dpSink.STATE_CONNECTED,
                                connectDeviceInfo);
                        btn_enable.setBackgroundResource(R.drawable.power_off);
                    } else {
                        Log.i(TAG, "start");

                        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                        if (adapter != null) {
                            if (adapter.getProfileConnectionState(BluetoothProfile.A2DP_SINK) !=
                                    BluetoothProfile.STATE_CONNECTED) {
                                Log.i(TAG, "No a2dp connected");
                                ShowToast("No A2dp connected!");
                                btn_enable.setBackgroundResource(R.drawable.power_off);
                                return false;
                            }
                        } else {
                            btn_enable.setBackgroundResource(R.drawable.power_off);
                            return false;
                        }
                        mPlayTask = new A2dpSinkWorkTask();
                        mPlayTask.execute();
                        btn_enable.setBackgroundResource(R.drawable.power_on);


                    }
                }
            }

            return false;
        }
    }
    private void AvrcpCmdSend(int cmd, int action) {
        if (a2dpState != BluetoothA2dpSink.STATE_CONNECTED)
            return;
        if (!isPlaying)
            return;
        if (mAvrcpControllerService == null)
            return;
        if (action == MotionEvent.ACTION_DOWN) {
            avrcpHandler.post(new AvrcpHandler(cmd, MainActivity.AVRCP_BTN_PRESS));
        } else if (action == MotionEvent.ACTION_UP) {
            avrcpHandler.post(new AvrcpHandler(cmd, MainActivity.AVRCP_BTN_RELEASE));
        }
    }

    class AvrcpButtonListener implements OnTouchListener {

        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            int img = 0, cmd;
            cmd = button_avrcp_cmd.get(v.getId());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                img = button_press_icon.get(id);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                img = button_release_icon.get(id);
            } else
                return false;
            AvrcpCmdSend(cmd, event.getAction());
            ImageButton btn = (ImageButton)findViewById(id);
            Drawable draw = getResources().getDrawable(img);
            btn.setBackgroundDrawable(draw);

            return false;
        }
    }
    private void initButtonDictionary() {
        AvrcpButtonListener listener = new AvrcpButtonListener();

        initButtonListener(R.id.btn_next,listener);
        initButtonListener(R.id.btn_pausei,listener);
        initButtonListener(R.id.btn_playi,listener);
        initButtonListener(R.id.btn_previous,listener);

        button_release_icon.clear();
        button_release_icon.put(R.id.btn_next, R.drawable.next);
        button_release_icon.put(R.id.btn_previous, R.drawable.previous);
        button_release_icon.put(R.id.btn_playi, R.drawable.play);
        button_release_icon.put(R.id.btn_pausei,R.drawable.pause);


        button_press_icon.clear();
        button_press_icon.put(R.id.btn_next, R.drawable.next_pressed);
        button_press_icon.put(R.id.btn_previous, R.drawable.previous_pressed);
        button_press_icon.put(R.id.btn_playi, R.drawable.play_pressed);
        button_press_icon.put(R.id.btn_pausei,R.drawable.pause_pressed);

        button_avrcp_cmd.clear();
        button_avrcp_cmd.put(R.id.btn_next, MainActivity.AVRCP_CMD_NEXT);
        button_avrcp_cmd.put(R.id.btn_previous, MainActivity.AVRCP_CMD_PREVIOUS);
        button_avrcp_cmd.put(R.id.btn_playi, MainActivity.AVRCP_CMD_PLAY);
        button_avrcp_cmd.put(R.id.btn_pausei, MainActivity.AVRCP_CMD_PAUSE);

        initPowerButton();


    }

    private void initPowerButton() {
        btn_enable = (ImageButton)findViewById(R.id.btn_enable);
        EnableButtonlistener listener = new EnableButtonlistener();
        btn_enable.setOnTouchListener(listener);

    }

    private void initButtonListener(int id, AvrcpButtonListener listener) {
        ImageButton btn = (ImageButton)findViewById(id);
        btn.setOnTouchListener(listener);
    }
    private void ShowToast(String info) {
        Toast toast = Toast.makeText(getApplicationContext(),info,Toast.LENGTH_SHORT);
        toast.show();
    }

    private void uiSetA2dpConnectState(boolean state, String info) {
        ImageView img = (ImageView)findViewById(R.id.img_a2dpState);
        Drawable btconnect = getApplication().getResources().getDrawable(R.drawable.a2dp_connected);
        Drawable btnoconnect = getApplication().getResources().getDrawable(R.drawable.a2dp_no_connected);
        TextView text = (TextView)findViewById(R.id.txt_a2dpState);

        if (state) {
            img.setBackground(btconnect);
            text.setText(info);
            this.connectDeviceInfo = info;
            a2dpState = BluetoothA2dpSink.STATE_CONNECTED;
            if (!isPlaying) {
                btn_start.setEnabled(true);
            } else
                btn_start.setEnabled(false);
        } else {
            img.setBackground(btnoconnect);
            text.setText(getString(R.string.noConnected));
            a2dpState = BluetoothA2dpSink.STATE_DISCONNECTED;
            btn_start.setEnabled(false);

        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiverA2dpSink);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int a = BluetoothProfile.A2DP_SINK;

        btn_start = (Button)findViewById(R.id.btn_start);
        btn_stop = (Button)findViewById(R.id.btn_stop);
        final BluetoothAdapter mBluetoothAdapter;
        initButtonDictionary();

        Log.i(TAG, "onCreate");
        btn_start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "start");

                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter != null) {
                    if (adapter.getProfileConnectionState(BluetoothProfile.A2DP_SINK) !=
                            BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "No a2dp connected");
                        ShowToast("No A2dp connected!");
                        return;
                    }
                } else
                    return;

                mPlayTask = new A2dpSinkWorkTask();
                mPlayTask.execute();
                btn_start.setEnabled(false);
            }
        });

        btn_stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = false;
                Log.i(TAG, "Stop!!!");
                uiSetA2dpConnectState(a2dpState == BluetoothA2dpSink.STATE_CONNECTED,
                        connectDeviceInfo);
            }
        });
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(intent);
            }

            if (mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP_SINK) !=
                    BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "no a2dp");
                ShowToast("No A2dp connected!");

            }
            else {
                a2dpState = BluetoothA2dpSink.STATE_CONNECTED;
                ShowToast("We have A2dp connection");
                Log.i(TAG, "we have a2dp");
            }
            if(mBluetoothAdapter.getProfileProxy(getApplicationContext(),
                    mA2dpListener,BluetoothProfile.A2DP_SINK)) {
                if (DBG)
                    Log.d(TAG, "getProfileProxy A2dpSink");
            } else
                Log.w(TAG, "cannot getProfileProxy A2dpSink");

            if(mBluetoothAdapter.getProfileProxy(getApplicationContext(),
                    mAvrcpControllerListener,BluetoothProfile.AVRCP_CONTROLLER)) {
                if (DBG)
                    Log.d(TAG, "getProfileProxy AvrcpController");
            } else
                Log.w(TAG, "cannot get profileProxy AvrcpController");

        }

        IntentFilter a2dpSinkIntentFilter = new IntentFilter();
        a2dpSinkIntentFilter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        a2dpSinkIntentFilter.addAction(BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED);
        a2dpSinkIntentFilter.addAction(BluetoothA2dpSink.ACTION_AUDIO_CONFIG_CHANGED);
        mBroadcastReceiverA2dpSink = new BroadcastReceiverA2dpSink();
        registerReceiver(mBroadcastReceiverA2dpSink,a2dpSinkIntentFilter);


    }

    @Override
    public void onStop() {
        super.onStop();
        isPlaying = false;
        if(DBG) Log.d(TAG, "onStop");
        uiSetA2dpConnectState(a2dpState == BluetoothA2dpSink.STATE_CONNECTED,
                connectDeviceInfo);
        btn_enable.setBackgroundResource(R.drawable.power_off);
        if (mTrack != null) {
            try {
                mTrack.stop();
            } catch (IllegalStateException err) {
                Log.e(TAG, err.toString());
            }
            mTrack.release();
        }
        if (mRecord != null) {
            try {
                mRecord.stop();
            } catch (IllegalStateException err) {
                Log.e(TAG, err.toString());
            }
            mRecord.release();
        }
        if (mPlayTask != null) {
            try {
                mPlayTask.cancel(true);
            } catch (Exception err) {
                Log.e(TAG, "onStop cannot cancel task because" + err);
            }
        }
        mTrack = null;
        mRecord = null;
    }

    class AvrcpHandler implements Runnable {
        int cmd = 0;
        int action = -1;

        public AvrcpHandler(int cmd, int action) {
            this.cmd = cmd;
            this.action = action;
        }
        public AvrcpHandler(int cmd) {
            this.cmd = cmd;
        }

        public void run() {
            if (DBG) Log.d(TAG, "will send avrcp " + cmd);
            if (mAvrcpControllerService != null) {
                List<BluetoothDevice> devices = mAvrcpControllerService.getConnectedDevices();
                for (BluetoothDevice device : devices) {
                    if (action == -1) {
                        mAvrcpControllerService.sendPassThroughCmd(device, cmd,
                                MainActivity.AVRCP_BTN_PRESS);
                        mAvrcpControllerService.sendPassThroughCmd(device, cmd, MainActivity.AVRCP_BTN_RELEASE);
                    } else {
                        mAvrcpControllerService.sendPassThroughCmd(device, cmd, action);
                    }
                }
            }
        }
    }


    class A2dpSinkWorkTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            Log.i(TAG, "background play");
            int minBufSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (DBG) Log.d(TAG, "background play 1");
            int minRecBufSize = AudioRecord.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            if (DBG) Log.d(TAG, "background play 2");
                mRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, minRecBufSize);
            if (DBG) Log.d(TAG, "background play 3");
                mTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        44100,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufSize,
                        AudioTrack.MODE_STREAM);

            if (DBG) Log.d(TAG, "background play 4");
            int ReadSize = minRecBufSize;
            byte[] recData = new byte[ReadSize];
            mTrack.play();
            mRecord.startRecording();
            isPlaying = true;
            while(isPlaying) {
                mRecord.read(recData, 0, ReadSize);
                mTrack.write(recData, 0, ReadSize);

            }
            if (mTrack != null) {
                try {
                    mTrack.stop();
                } catch (IllegalStateException err) {
                    Log.e(TAG, "playback task " + err.toString());
                }
                mTrack.release();
            }
            if (mRecord != null) {
                try {
                    mRecord.stop();
                } catch (IllegalStateException err) {
                    Log.e(TAG, "playback task " + err.toString());
                }
                mRecord.release();
            }
            mTrack = null;
            mRecord = null;
            Log.i(TAG, "stop on task!");
            return null;
        }
    }

    private BluetoothProfile.ServiceListener mA2dpListener
            = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (DBG) Log.d(TAG, "A2dpSink Service connected profile = "
                    + profile + " proxy = " +
                    proxy);
            if (profile == BluetoothProfile.A2DP_SINK) {
                mA2dpSinkService = (BluetoothA2dpSink)proxy;
            } else {
                Log.w(TAG, "Wrong profile connected on A2dpListener");
                return;
            }
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            boolean state = (adapter.getProfileConnectionState(BluetoothProfile.A2DP_SINK) ==
                    BluetoothProfile.STATE_CONNECTED);
            String info = null;
            if (state) {
                List<BluetoothDevice> devices = mA2dpSinkService.getConnectedDevices();
                for (BluetoothDevice device : devices) {
                    info = device.getAliasName() + " " + device.getAddress();
                }
            }
            uiSetA2dpConnectState(state,info);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (DBG) Log.d(TAG, "A2dpSink Service disconnect profile = " + profile);
        }
    };

    private BluetoothProfile.ServiceListener mAvrcpControllerListener =
            new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG,"AvrcpControllerListener connected profile = "
                    + profile + " proxy = "
            + proxy);

            if (profile == BluetoothProfile.AVRCP_CONTROLLER) {
                mAvrcpControllerService = (BluetoothAvrcpController)proxy;
            } else
                Log.w(TAG, "AvrcpControllerListener wrong profile");
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "AvrcpControllerListener disconnect profile = " + profile);
        }
    };


    private class BroadcastReceiverA2dpSink extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "A2dpSink Broadcast Received");
            String action = intent.getAction();
            if (action == BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED) {
                Log.d(TAG, "BluetoothA2dpSink connection State Changed!");
                int state = intent.getIntExtra(BluetoothA2dpSink.EXTRA_STATE,-1);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int previous_state =
                        intent.getIntExtra(BluetoothA2dpSink.EXTRA_PREVIOUS_STATE,-1);
                String info = null;
                boolean connected = false;
                if (state == BluetoothA2dpSink.STATE_CONNECTED) {
                    ShowToast("A2dp source connected!");
                    connected = true;
                    info = device.getAliasName() + " " + device.getAddress();
                    a2dpRemoteDevice = device;
                } else {
                    ShowToast("A2dp source disconnected!");
                    if (isPlaying) {
                        isPlaying = false;
                        if (btn_enable !=null)
                            btn_enable.setBackgroundResource(R.drawable.power_off);
                    }
                    a2dpRemoteDevice = null;
                }

                uiSetA2dpConnectState(connected,info);

            }
            if (action == BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED) {
                Log.d(TAG, "BluetoothA2dpSink Playing State Changed!");
                int state = intent.getIntExtra(BluetoothA2dpSink.EXTRA_STATE,-1);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int previous_state =
                        intent.getIntExtra(BluetoothA2dpSink.EXTRA_PREVIOUS_STATE,-1);
                if (state == BluetoothA2dpSink.STATE_PLAYING) {
                    ShowToast("A2dp Source is playing!");
                } else {
                    ShowToast("A2dp Source paused.");
                }

            }
        }
    }
}
