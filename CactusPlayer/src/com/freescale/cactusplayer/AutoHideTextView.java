/* Copyright 2012-2013 Freescale Semiconductor, Inc.
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

package com.freescale.cactusplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class AutoHideTextView extends TextView {
    private String TAG = "CactusPlayer";
    private static final String CLASS = "AutoHideTextView: ";

	private boolean mPause = false;
	private int     mDuration = 0;

    private Handler mTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
			// one second elapsed
			if(!mPause)
				mDuration -= 1000;
			// if time out, hide; otherwise, check one second later
			if(mDuration <= 0)
				setVisibility(View.INVISIBLE);
			else
				mTimerHandler.sendEmptyMessageDelayed(1, 1000);
        }
    };

	public AutoHideTextView(Context context) {
		super(context);
	}

	public AutoHideTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AutoHideTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void pause() {
		mPause = true;
	}

	public void resume() {
		mPause = false;
	}

    public void setText(String text, int duration) {
		// change text
		super.setText(text);

		mDuration = duration;

        // cancel any previous messages
		mTimerHandler.removeMessages(1);

        if(text != null)
    	{
			// show me
			setVisibility(View.VISIBLE);

            if(duration > 0)
        	{
		        // set timer
				mTimerHandler.sendEmptyMessageDelayed(1, 1000);
        	}
    	}
		else
		{
			setVisibility(View.INVISIBLE);
		}
	}
}

