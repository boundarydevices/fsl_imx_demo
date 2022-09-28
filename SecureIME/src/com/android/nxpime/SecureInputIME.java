package com.android.nxpime;

import androidx.appcompat.app.AppCompatActivity;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.util.Log;

import android.os.Bundle;
import android.view.Surface;
import android.widget.TextView;

import java.io.FileDescriptor;

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
		} else
			Log.e(TAG, "Can not get valid InputConnection in onWindowShown()!");
	}

	@Override
	public void onWindowHidden() {
		super.onWindowHidden();
		ic = null;
		if (getWindow() != null)
			getWindow().hide();
	}

	@Override
	public boolean onEvaluateFullscreenMode() {
		return false;
	}

	@Override
	public boolean onEvaluateInputViewShown() {
		EditorInfo info = getCurrentInputEditorInfo();
		if (info.inputType != (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)) {
			switchToNextInputMethod(false);
			return false;
		} else {
			return super.onEvaluateInputViewShown();
		}
	}
}
