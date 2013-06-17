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

import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;

public class VideoView extends SurfaceView {
    private String TAG = "CactusPlayer";
	private static final String CLASS = "VideoView: ";

    private int         mVideoWidth;
    private int         mVideoHeight;
    private float       mOldPar;

    private void initVideoView() {
        mVideoWidth     = 0;
        mVideoHeight    = 0;
		mOldPar         = (float) 0.0;
//        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//		getHolder().setFixedSize(mVideoWidth, mVideoHeight); // default to 1:1
    }

    Surface getSurface() {
		return getHolder().getSurface();
	}
/*
    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback()
    {
        public void surfaceChanged(SurfaceHolder holder, int format,
                                    int w, int h)
        {
            mSurfaceWidth = w;
            mSurfaceHeight = h;
        }

        public void surfaceCreated(SurfaceHolder holder)
        {
            mSurfaceHolder = holder;
        }

        public void surfaceDestroyed(SurfaceHolder holder)
        {
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
			// TODO: release
        }
    };
*/
    public VideoView(Context context) {
        super(context);
        initVideoView();
    }

    public VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        initVideoView();
    }

    public VideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initVideoView();
    }

	// onMeasure will be called when surface is created or size changed
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(TAG, "onMeasure " + "mVideoW " + mVideoWidth + " mVideoH " + mVideoHeight
                + " widthMesaureSpec " + widthMeasureSpec + " heightMeasureSpec " + heightMeasureSpec);

		// to reduce flicker at startup, create a small content area
		if(mVideoWidth == 0 || mVideoHeight == 0) { // no video now
			setMeasuredDimension(1, 1);
			return;
		}

		int width  = getDefaultSize(mVideoWidth,  widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

		if (width > 0 && height > 0 && mVideoWidth > 0 && mVideoHeight > 0) {
			float win_asp = (float)width / height;
			float pic_asp = (float)mVideoWidth / mVideoHeight;
			if ( pic_asp > win_asp ) {
				height = (int)((float)width / pic_asp);
			} else if ( pic_asp < win_asp ) {
				width = (int)((float)height * pic_asp);
			}

			setMeasuredDimension(width, height); // set view's content size to maintain same aspect ratio as video sepcified
		}
		else {
			// bad parameters
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	public void onVideoSizeChanged(int newWidth, int newHeight, int sarW, int sarH) {
		Log.d(TAG, "onVideoSizeChanged: " + newWidth + " x " + newHeight + ", (" + sarW + "/" + sarH + ")");
		// TODO: will support non-square pixel display later

		float sar = (float)sarW / sarH;

        if(sar >= 1.0f) {
			mVideoWidth	 = (int)(sar * newWidth + 0.5);
			mVideoHeight = newHeight;
    	}
		else {
			mVideoWidth	 = newWidth;
			mVideoHeight = (int)(newHeight / sar + 0.5);
		}

		// video size changed, but view content size is unchanged if picture aspect ratio is unchanged
		float par = (float)mVideoWidth/mVideoHeight;
		float diff = par - mOldPar;
		if(diff < 0) diff = -diff;
		float distortion = diff / par;
		if(mOldPar == 0 || distortion > 0.05) {
			// will call onMeasure, and then surfaceChanged(size is same as setFixedSize)
			getHolder().setFixedSize(mVideoWidth, mVideoHeight);
			mOldPar = par;
		}
	}
}

