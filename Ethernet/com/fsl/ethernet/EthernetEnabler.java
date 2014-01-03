/*
 * Copyright (C) 2013-2014 Freescale Semiconductor, Inc.
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
import android.content.Context;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import com.fsl.ethernet.EthernetManager;
/**
 * Created by B38613 on 9/27/13.
 */
public class EthernetEnabler {
    public static final String TAG = "SettingsEthEnabler";

    private Context mContext;
    private EthernetManager mEthManager;
    private EthernetConfigDialog mEthConfigDialog;
    private EthernetAdvDialog mEthAdvancedDialog;

    public void setConfigDialog (EthernetConfigDialog Dialog) {
        mEthConfigDialog = Dialog;
    }

    public void setmEthAdvancedDialog(EthernetAdvDialog Dialog) {
        this.mEthAdvancedDialog = Dialog;
    }

    public EthernetEnabler(Context context) {
        mContext = context;
        mEthManager = new EthernetManager(context);

        mEthManager.resetInterface();
    }

    public EthernetManager getManager() {
        return mEthManager;
    }
    public void resume() {
    }

    public void pause() {
    }

    public void setEthEnabled() {

        if (mEthManager.isConfigured() != true) {
            mEthConfigDialog.show();
        } else {
            mEthManager.resetInterface();
        }
    }
}
