/*
 * Copyright  2007 The Android Open Source Project
 * Copyright (C) 2013 Freescale Semiconductor, Inc.
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


package com.freescale.HdmiDualVideo;

import android.widget.VideoView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRouter.RouteInfo;
import android.media.MediaRouter;
import android.app.Application;
import android.app.Presentation;
import java.io.File;
import java.io.FilenameFilter;
import android.os.Bundle;
import java.util.List;
import java.util.ArrayList;
import android.net.Uri;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.view.Display;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.WindowManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import android.os.SystemProperties;
import android.content.res.Resources;
import android.graphics.Color;


public class HdmiApplication extends Application
{
    private static final String TAG = "HdmiApplication";
    private MediaRouter mMediaRouter = null;
    private DemoPresentation mPresentation = null;
    private Uri mVideoUri;
    public boolean mVideoRunning = false;

    public interface Callback {
        public void onPresentationChanged(boolean change);
    }

    private Callback mListener = null;

    public void addListener(Callback listerner) {
        mListener = listerner;
    }

    public DemoPresentation getPresentation() {
        return mPresentation;
    }

    public void onCreate() {
        super.onCreate();

        mMediaRouter = (MediaRouter)getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        updatePresentation();
    }

    private final MediaRouter.SimpleCallback mMediaRouterCallback =
            new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
            Log.w(TAG, "onRouteSelected: type=" + type + ", info=" + info);
            updatePresentation();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
            Log.w(TAG, "onRouteUnselected: type=" + type + ", info=" + info);
            updatePresentation();
        }

        @Override
        public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo info) {
            Log.w(TAG, "onRoutePresentationDisplayChanged: info=" + info);
            updatePresentation();
        }
    };

    private void updatePresentation() {
        // Get the current route and its presentation display.
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute(
                MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;

        // Dismiss the current presentation if the display has changed.
        if (mPresentation != null && mPresentation.getDisplay() != presentationDisplay) {
            Log.w(TAG, "Dismissing presentation because the current route no longer "
                    + "has a presentation display.");
            //mPresentation.stopVideo();
            if (mListener != null) {
                mListener.onPresentationChanged(false);
            }
            mPresentation.dismiss();
            mPresentation = null;
        }

        // Show a new presentation if needed.
        if (mPresentation == null && presentationDisplay != null) {
            Log.w(TAG, "Showing presentation on display: " + presentationDisplay);
            mPresentation = new DemoPresentation(this, presentationDisplay);
            try {
                WindowManager.LayoutParams l = mPresentation.getWindow().getAttributes();
                //l.type = WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW + 100;
                l.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                mPresentation.show();
                if (mListener != null) {
                    mListener.onPresentationChanged(true);
                }
                }catch(WindowManager.InvalidDisplayException ex) {
                    Log.w(TAG, "Couldn't show presentation!  Display was removed in "
                        + "the meantime.", ex);
                    mPresentation = null;
                }
        }
    }

    /**
     * The presentation to show on the secondary display.
     * <p>
     * Note that this display may have different metrics from the display on which
     * the main activity is showing so we must be careful to use the presentation's
     * own {@link Context} whenever we load resources.
     * </p>
     */
    public final  class DemoPresentation extends Presentation {
        private VideoView mvideoview1; 
        private VideoView mvideoview2; 
        private String mVideoFile1 = "";
        private String mVideoFile2 = "";
        private Uri mUri1;
        private Uri mUri2;
        private ProgressBar mProgress1;
        private ProgressBar mProgress2;
        private TextView mTextView1;
        private TextView mTextView2;
        private boolean flag = true;
        private boolean mVideo2Created = false;
        private Handler mHandler = new MainHandler();
        private final int UPDATE = 3580;
        private final int  CREAT_SECOND_VIDEO = 3589;

        public VideoView getMvideoview1() {
            return mvideoview1;
        }

        public void setMvideoview1(VideoView mvideoview1) {
            this.mvideoview1 = mvideoview1;
        }

        public VideoView getMvideoview2() {
            return mvideoview2;
        }

        public void setMvideoview2(VideoView mvideoview2) {
            this.mvideoview2 = mvideoview2;
        }

        public ProgressBar getmProgress1() {
            return mProgress1;
        }

        public void setmProgress1(ProgressBar mProgress1) {
            this.mProgress1 = mProgress1;
        }

        public ProgressBar getmProgress2() {
            return mProgress2;
        }

        public void setmProgress2(ProgressBar mProgress2) {
            this.mProgress2 = mProgress2;
        }

        private class MainHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                   case UPDATE: { 
                      getmProgress1().setProgress(mvideoview1.getCurrentPosition());
                      getmProgress2().setProgress(mvideoview2.getCurrentPosition());
                      getTextView1().setText("FullScreen: " + mvideoview1.getCurrentPosition()/1000 + "s" 
                             + " " + "SmallScreen: "  + mvideoview2.getCurrentPosition()/1000 + "s");
                      getTextView1().setBackgroundColor(Color.argb(55, 255, 255, 255));
                      getTextView1().setTextColor(Color.argb(150, 105, 200, 255));
                   break;
                        }

                   case CREAT_SECOND_VIDEO:
                      mvideoview2 = (VideoView)findViewById(R.id.videoview2);
                      if(mVideoFile2.contentEquals("blank"))
                      {
                      Log.i(TAG, "VideoFile:blank ");
                      mvideoview2.setVisibility(View.INVISIBLE);
                      }else{
                          SystemProperties.set("rw.VIDEO_RENDER_NAME", "video_render.surface");
                          mUri2 = Uri.fromFile(new File(mVideoFile2));
                          mvideoview2.setVideoURI(mUri2);
                          mvideoview2.requestFocus();
                          mvideoview2.start();      
                          Log.i(TAG, "VideoFile= " + mUri2);
    
                          mvideoview2.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                              @Override
                              public void onCompletion(MediaPlayer mp)  {
                                  SystemProperties.set("rw.VIDEO_RENDER_NAME", "video_render.surface");
                                  mvideoview2.start();
                              }
                          });
                          mvideoview2.setOnErrorListener(new MediaPlayer.OnErrorListener(){
                              @Override
                              public boolean onError(MediaPlayer mp, int what, int extra){
                              mUri2 = Uri.fromFile(new File(mVideoFile2));
                              mvideoview2.setVideoURI(mUri2);
                              mvideoview2.start();
                              return true;
                              }
                          });
                      }

                        break;
                    default:
                    break;
                    }
                }
        }

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
            mProgress1 = (ProgressBar) findViewById(R.id.progressBar1);
            mProgress2 = (ProgressBar) findViewById(R.id.progressBar2);
            mTextView1 = (TextView)findViewById(R.id.textView1);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (flag) {
                        if(mVideo2Created ==  true) {
                            mHandler.sendEmptyMessage(UPDATE);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (mVideoRunning) {
                            if(mVideo2Created ==  false) {
                                mVideo2Created = true;
                                mHandler.sendEmptyMessage(CREAT_SECOND_VIDEO);
                            }
                        }
                    }  
                }
            }).start();
        }


        public TextView getTextView1() {
           return mTextView1;
        }

        public void setTextView1(TextView textView1) {
        this.mTextView1 = textView1;
        }


        @Override
        protected void onStop() {
            super.onStop();
            mvideoview1.stopPlayback();
            mvideoview2.stopPlayback();
            flag = false;
        }


        public void startVideo(String video1, String video2) {
            if (mVideoRunning == true) {
                mvideoview1.stopPlayback();
                mvideoview2.stopPlayback();
                mVideo2Created = false; 
            }
            mVideoRunning = true;
            mVideoFile1 = video1;
            mVideoFile2 = video2;
            Log.i(TAG, "startVideo: video1=" + ", video2=" + video2);
            if (mVideoFile1.contentEquals("blank"))
            {
                Log.i(TAG, "VideoFile1:blank ");
                mvideoview1.setVisibility(View.INVISIBLE);
            }else{
                SystemProperties.set("rw.VIDEO_RENDER_NAME", "video_render.ipulib");
                mUri1 = Uri.fromFile(new File(mVideoFile1));
                mvideoview1.setVideoURI(mUri1);
                mvideoview1.requestFocus();
                mvideoview1.start();   
                Log.i(TAG, "VideoFile1= " + mUri1);

                mvideoview1.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                    @Override
                    public void onCompletion(MediaPlayer mp)  {
                         SystemProperties.set("rw.VIDEO_RENDER_NAME", "video_render.ipulib");
                         mvideoview1.start();
                    }
                });
                mvideoview1.setOnErrorListener(new MediaPlayer.OnErrorListener(){
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra){
                        mUri1 = Uri.fromFile(new File(mVideoFile1));
                        mvideoview1.setVideoURI(mUri1);
                        mvideoview1.start();
                        return true;
                    }
                });
            }
        }

        public void pauseVideo() {
            if (mVideoRunning) {
                mVideoRunning = false;
                mvideoview1.pause();
                if(mVideo2Created ==  true) 
                    mvideoview2.pause();
            }
        }

        public void resumeVideo() {
            if (mVideoRunning ==  false) {
                mVideoRunning = true;
                mvideoview1.start();
                if(mVideo2Created ==  true) 
                    mvideoview2.start();
            }
        }

        public void stopVideo() {
            if (mVideoRunning) {
                mVideoRunning = false;
                mVideo2Created =  false;
                mvideoview1.stopPlayback();
                mvideoview2.stopPlayback();
            }
        }

        public void onDisplayRemoved() {
            Log.i(TAG, "----onDisplayRemoved----");
            stopVideo();
        }
    }
}

