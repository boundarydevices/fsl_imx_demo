/*
 * Copyright (C) 2015 Boundary Devices, Inc.
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

package com.fsl.ethernet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.util.Log;
import com.fsl.ethernet.EthernetManager;

public class EthernetReceiver extends BroadcastReceiver {

    private static final String TAG = "EthernetReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive " + intent.getAction());

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "resetting interface");
            EthernetManager ethManager = new EthernetManager(context);
            ethManager.resetInterface();
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            NetworkInfo info =
                intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (info != null) {
                if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    Log.i(TAG,"Ethernet state: " + info.getState());
                    if (info.getState() == State.DISCONNECTED) {
                        ConnectivityManager connMgr = (ConnectivityManager)context.
                            getSystemService(Context.CONNECTIVITY_SERVICE);
                        connMgr.setGlobalProxy(null);
                    } else if (info.getState() == State.CONNECTED) {
                        EthernetManager ethManager = new EthernetManager(context);
                        ethManager.resetInterface();
                        ethManager.initProxy();
                    }
                }
            }
        }
    }
}
