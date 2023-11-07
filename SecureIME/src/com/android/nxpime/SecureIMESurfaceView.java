package com.android.nxpime;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class SecureIMESurfaceView extends SurfaceView implements Callback {
    private SurfaceHolder mHolder;
    private Thread t;
    private boolean flag;
    private int x, y;
    private static final String TAG = "SecureIMESurfaceView";
    private InputConnection mIC;
    private int mSurfaceWidth = 720, mSurfaceHeight = 400;

    public SecureIMESurfaceView(Context context) {
        super(context);

        this.setFilterTouchesWhenObscured(true);

        /* connect to AIDL service */
        connectAIDL();

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setFixedSize(mSurfaceWidth, mSurfaceHeight);
    }

    public void setInputConnection(InputConnection ic) {
        mIC = ic;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "in surfaceCreated!");
        startKeyboard(mHolder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "in surfaceDestroyed!");
        cleanup();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int key;
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                x = (int) event.getX(); // get X coordinate
                y = (int) event.getY(); // get Y coordinate
                key = deliverInput(x, y);
                /* only allow valid key */
                Log.i(TAG, "coordinate: X " + x + " Y " + y);
                if ((key >= 0) && (key <= 9)) {
                    mIC.commitText(String.valueOf(key), 1);
                } else if (key == 10) {
                    mIC.deleteSurroundingText(1, 0);
                } else if (key == 11) {
                    mIC.performEditorAction(EditorInfo.IME_ACTION_DONE);
                } else {
                    Log.e(TAG, "in surface onTouchEvent! Invalid touch input " + key + " abandon!");
                }
        }
        return true;
    }

    /* connect AIDL service */
    private native int connectAIDL();

    /* create surface and init keyboard */
    private native int startKeyboard(Surface surface);

    /* deliver coordinate to TEE and return the number */
    private native int deliverInput(int x, int y);

    /* clean up resource on exit */
    private native int cleanup();

    static {
        System.loadLibrary("secureime_jni");
    }
}
