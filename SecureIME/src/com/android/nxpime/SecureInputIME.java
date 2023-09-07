package com.android.nxpime;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class SecureInputIME extends InputMethodService {

    private InputConnection ic;
    SecureIMESurfaceView imeSurfaceview = null;
    private static final String TAG = "SecureIME";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public View onCreateInputView() {
        imeSurfaceview = new SecureIMESurfaceView(getApplicationContext());
        return imeSurfaceview;
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        ic = getCurrentInputConnection();
        if (ic != null && imeSurfaceview != null) {
            imeSurfaceview.setInputConnection(ic);
        } else Log.e(TAG, "Can not get valid InputConnection in onWindowShown()!");
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        ic = null;
        if (getWindow() != null) getWindow().hide();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info.inputType
                != (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)) {
            switchToNextInputMethod(false);
            return false;
        } else {
            return super.onEvaluateInputViewShown();
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        ic = getCurrentInputConnection();
        if (ic != null && imeSurfaceview != null) {
            imeSurfaceview.setInputConnection(ic);
        } else Log.e(TAG, "Can not get valid InputConnection in onStartInput!");
    }
}
