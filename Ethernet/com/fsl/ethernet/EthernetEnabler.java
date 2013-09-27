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

    public void setConfigDialog (EthernetConfigDialog Dialog) {
        mEthConfigDialog = Dialog;
    }

    public EthernetEnabler(Context context) {
        mContext = context;
        mEthManager = new EthernetManager(context);
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
