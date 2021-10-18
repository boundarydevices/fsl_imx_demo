/*
 * Copyright 2021 NXP
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

package com.nxp.ethernet;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final String TAG = "EthernetMainActivity";

    private EthernetDevInfo  mSaveConfig;
    private EthernetProxyDialog mEthernetProxyDialog;
    private EthernetEnabler mEthEnabler;
    private EthernetIpDialog mEthIpConfigDialog;

    private EthernetManager mEthernetManager;
    private int mNetworkType;
    private ConnectivityManager mConnectivityManager;

    private final BroadcastReceiver mEthernetReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            updateConnectivityStatus();
            updateConnectivity();
        }
    };

    private void updateConnectivityStatus() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            mNetworkType = ConnectivityManager.TYPE_NONE;
        } else {
            if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                mNetworkType = ConnectivityManager.TYPE_ETHERNET;
            } else {
                mNetworkType = ConnectivityManager.TYPE_NONE;
            }
        }
    }

    /**
     * Return whether Ethernet port is available.
     */
    public boolean isEthernetAvailable() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_ETHERNET)
                && mEthernetManager.getAvailableInterfaces().length > 0;
    }

    public boolean isEthernetConnected() {
        return mNetworkType == ConnectivityManager.TYPE_ETHERNET;
    }

    private void updateConnectivity() {
        final boolean ethernetAvailable = isEthernetAvailable();
        if (ethernetAvailable) {
            final boolean ethernetConnected = isEthernetConnected();
            TextView text = (TextView) findViewById(R.id.tvConfig);
            if (!ethernetConnected) {
                text.setText(R.string.eth_not_connected);
            } else {
                text.setMovementMethod(ScrollingMovementMethod.getInstance());
                text.setText(getEthernetIpAddress());
            }
        }
    }

    private Network getFirstEthernet() {
        final Network[] networks = mConnectivityManager.getAllNetworks();
        for (final Network network : networks) {
            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                return network;
            }
        }
        return null;
    }

    /**
     * Returns the formatted IP addresses of the Ethernet connection or null
     * if none available.
     */
    public String getEthernetIpAddress() {
        final Network network = getFirstEthernet();
        if (network == null) {
            return null;
        }
        return formatIpAddresses(network);
    }

    private String formatIpAddresses(Network network) {
        final LinkProperties linkProperties = mConnectivityManager.getLinkProperties(network);
        if (linkProperties == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        boolean gotAddress = false;
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            if (gotAddress) {
                sb.append("\n");
            }
            sb.append(linkAddress.getAddress().getHostAddress());
            gotAddress = true;
        }
        if (gotAddress) {
            return sb.toString();
        } else {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.ethernet_configure);

        mEthernetManager = (EthernetManager) getSystemService(EthernetManager.class);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mEthEnabler = new EthernetEnabler(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mEthernetReceiver, filter);

        Button mBtnCheck = (Button) findViewById(R.id.btnCheck);
        Button mBtnIPConfig = (Button) findViewById(R.id.btnIPConfig);
        Button mBtnProxyConfig = (Button) findViewById(R.id.btnProxyConfig);

        mBtnCheck.setOnClickListener(v -> updateConnectivity());

        mBtnIPConfig.setOnClickListener(v -> {
            mEthIpConfigDialog = new EthernetIpDialog(MainActivity.this, mEthEnabler);
            mEthEnabler.setIpDialog(mEthIpConfigDialog);
            mEthIpConfigDialog.show();
        });

        mBtnProxyConfig.setOnClickListener(v -> {
            mSaveConfig = mEthEnabler.getManager().getSavedConfig();
            if (mSaveConfig != null) {
                mEthernetProxyDialog = new EthernetProxyDialog(MainActivity.this,mEthEnabler);
                mEthEnabler.setProxyDialog(mEthernetProxyDialog);
                mEthernetProxyDialog.show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // There doesn't seem to be an API to listen to everything this could cover, so
        // tickle it here and hope for the best.
        updateConnectivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onStop() will force clear global proxy set by ethernet");
        unregisterReceiver(mEthernetReceiver);
    }
}
