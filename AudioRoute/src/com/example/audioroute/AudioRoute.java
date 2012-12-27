/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.audioroute;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.media.AudioManager;
import android.media.AudioSystem;

public class AudioRoute extends Activity {

    private final static String TAG = "AudioRoute";

    private TextView mCommand;
    private CheckBox mSpdifIn;

    private AudioManager mAudioManager;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create and attach the view that is responsible for painting.

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setContentView(R.layout.main);
        mCommand = (TextView) findViewById(R.id.commandText);
        mSpdifIn = (CheckBox)findViewById(R.id.spdif_in);


        mCommand.setText("Please select input device");

        mSpdifIn.setText("Enable SPDIF In");

        if (AudioSystem.getDeviceConnectionState(AudioSystem.DEVICE_IN_AUX_DIGITAL, "") == AudioSystem.DEVICE_STATE_AVAILABLE)
            mSpdifIn.setChecked(true);
        else
            mSpdifIn.setChecked(false);

        mSpdifIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "SPDIF IN is clicked");

                setupAudioRoute();
            }
        });

    }

    private void setupAudioRoute() {
        Log.d(TAG, "SPDIF IN "+ mSpdifIn.isChecked());
        mAudioManager.setWiredDeviceConnectionState(AudioSystem.DEVICE_IN_AUX_DIGITAL, mSpdifIn.isChecked() ? 1 : 0, "");
    }

}
