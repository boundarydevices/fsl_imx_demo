/*
 * Copyright  2007 The Android Open Source Project
 * Copyright  2013 Freescale Semiconductor, Inc.
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

package com.freescale.test_dualplayback;

import android.os.Bundle;

import android.app.Activity;
import android.view.Menu;

import java.io.File;


import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;


import android.app.MediaRouteActionProvider;
import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaRouter;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.VideoView;

public class SecondActivity extends Activity {
    private final String TAG = "PresentationWithMediaRouterActivity";

    private MediaRouter mMediaRouter;
    private DemoPresentation mPresentation;
    private VideoView mvideoview1;
    private VideoView mvideoview2;
    private boolean mPaused;
    private static Uri			mUri1;
    private static Uri			mUri2;
    private String VideoFile = "";
    private String VideoFile1 = "";
    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call {@link android.app.Activity#setContentView setContentView()} to
     * describe what is to be displayed in the screen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getIntent().getExtras();
        VideoFile = bundle.getString("videofile0");
        VideoFile1 = bundle.getString("videofile1");
        // Get the media router service.
        mMediaRouter = (MediaRouter)getSystemService(Context.MEDIA_ROUTER_SERVICE);

        // See assets/res/any/layout/presentation_with_media_router_activity.xml for this
        // view layout definition, which is being set here as
        // the content of our screen.
        setContentView(R.layout.presentation_with_media_router_activity);

        // Set up the surface view for visual interest.
        mvideoview1 = (VideoView)findViewById(R.id.videoview1);
       // mSurfaceView.setRenderer(new CubeRenderer(false));
      //  VideoFile = "/storage/emulated/legacy/"+"Mpeg4_SP1_1920x1080_30fps_9346kbps_MP3_48KHz_64kbps_2ch.mp4";
      //  VideoFile1 = "/storage/emulated/legacy/"+"H264_HP41_1920x1088_30fps_55.8Mbps_shields_ter.mp4";
        mUri1 = Uri.fromFile(new File(VideoFile1));
        mvideoview2 = (VideoView)findViewById(R.id.videoview2);
        mUri2 = Uri.fromFile(new File(VideoFile));
        mvideoview2.setVideoURI(mUri2);
        mvideoview2.requestFocus();
	    mvideoview2.start();
	    mvideoview2.setOnErrorListener(new MediaPlayer.OnErrorListener(){
	    	public boolean onError(MediaPlayer mp, int what, int extra){
	    		VideoFile = "/storage/emulated/legacy/"+"Mpeg4_SP1_1920x1080_30fps_9346kbps_MP3_48KHz_64kbps_2ch.mp4";
	    		mUri2 = Uri.fromFile(new File(VideoFile));
	    		mvideoview2.setVideoURI(mUri2);
	    		mvideoview2.start();
	    		return true;
	    	}
	    } );
	    mvideoview2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
 @Override
public void onCompletion(MediaPlayer mp) {
	    		 VideoFile = "/storage/emulated/legacy/"+"Mpeg4_SP1_1920x1080_30fps_9346kbps_MP3_48KHz_64kbps_2ch.mp4";
	 mvideoview2.start();
	    	                   }
	    });
        Log.i(TAG,"onCreate set video0");

    }

    @Override
    protected void onResume() {
        // Be sure to call the super class.
        super.onResume();

        // Listen for changes to media routes.
        mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);

        // Update the presentation based on the currently selected route.
        mPaused = false;
        updatePresentation();
    }

    @Override
    protected void onPause() {
        // Be sure to call the super class.
        super.onPause();

        // Stop listening for changes to media routes.
        mMediaRouter.removeCallback(mMediaRouterCallback);

        // Pause rendering.
        mPaused = true;
        updateContents();
    }

    @Override
    protected void onStop() {
        // Be sure to call the super class.
        super.onStop();

        // Dismiss the presentation when the activity is not visible.
        if (mPresentation != null) {
            Log.i(TAG, "Dismissing presentation because the activity is no longer visible.");
            mPresentation.dismiss();
            mPresentation = null;
        }
    }
    private void updatePresentation() {
        // Get the current route and its presentation display.
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute(
                MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;

        // Dismiss the current presentation if the display has changed.
        if (mPresentation != null && mPresentation.getDisplay() != presentationDisplay) {
            Log.i(TAG, "Dismissing presentation because the current route no longer "
                    + "has a presentation display.");
            mPresentation.dismiss();
            mPresentation = null;
        }

        // Show a new presentation if needed.
        if (mPresentation == null && presentationDisplay != null) {
            Log.i(TAG, "Showing presentation on display: " + presentationDisplay);
            mPresentation = new DemoPresentation(this, presentationDisplay);
            mPresentation.setOnDismissListener(mOnDismissListener);
            try {
                mPresentation.show();
            } catch (WindowManager.InvalidDisplayException ex) {
                Log.w(TAG, "Couldn't show presentation!  Display was removed in "
                        + "the meantime.", ex);
                mPresentation = null;
            }
        }

        // Update the contents playing in this activity.
        updateContents();
    }

    private void updateContents() {
        // Show either the content in the main activity or the content in the presentation
        // along with some descriptive text about what is happening.
        if (mPresentation != null) {
            mvideoview1.setVisibility(View.INVISIBLE);
            mvideoview1.pause();
            if (mPaused) {
                mPresentation.getSurfaceView().pause();
            } else {
                mPresentation.getSurfaceView().resume();
            }
        } else {
            mvideoview1.setVisibility(View.VISIBLE);
            if (mPaused) {
                mvideoview1.pause();
            } else {
                mvideoview1.resume();
            }
        }
    }

    private final MediaRouter.SimpleCallback mMediaRouterCallback =
            new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
            Log.d(TAG, "onRouteSelected: type=" + type + ", info=" + info);
            updatePresentation();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: type=" + type + ", info=" + info);
            updatePresentation();
        }

        @Override
        public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRoutePresentationDisplayChanged: info=" + info);
            updatePresentation();
        }
    };

    /**
     * Listens for when presentations are dismissed.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            if (dialog == mPresentation) {
                Log.i(TAG, "Presentation was dismissed.");
                mPresentation = null;
                updateContents();
            }
        }
    };

    /**
     * The presentation to show on the secondary display.
     * <p>
     * Note that this display may have different metrics from the display on which
     * the main activity is showing so we must be careful to use the presentation's
     * own {@link Context} whenever we load resources.
     * </p>
     */
    private final  class DemoPresentation extends Presentation {
        private VideoView mvideoview1;

        public DemoPresentation(Context context, Display display) {
            super(context, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // Be sure to call the super class.
            super.onCreate(savedInstanceState);

            // Get the resources for the context of the presentation.
            // Notice that we are getting the resources from the context of the presentation.
            Resources r = getContext().getResources();

            // Inflate the layout.
            setContentView(R.layout.presentation_with_media_router_content);

            // Set up the surface view for visual interest.
            mvideoview1 = (VideoView)findViewById(R.id.videoview1);
            mvideoview1.setVideoURI(mUri1);
            mvideoview1.requestFocus();
    	    mvideoview1.start();
    	    mvideoview1.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
@Override
             public void onCompletion(MediaPlayer mp)  {
                         mvideoview1.start();
              }
    	    });
    	    mvideoview1.setOnErrorListener(new MediaPlayer.OnErrorListener(){
              @Override
    	       public boolean onError(MediaPlayer mp, int what, int extra){
    	    	    mUri1 = Uri.fromFile(new File(VideoFile1));
    	    	    mvideoview1.setVideoURI(mUri1);
    	    	    mvideoview1.start();
    	    	    return true;
    	    	 }
    	    });
        }

        public VideoView getSurfaceView() {
            return mvideoview1;
        }
    }
}


