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

import java.io.File;

import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.app.Presentation;

public class MainActivity extends Activity implements OnClickListener, HdmiApplication.Callback
 {
    private HdmiApplication mHdmiApp;
    private HdmiApplication.DemoPresentation mPresentation;
    private Button start_button;
    private Button stop_button;
    private String[] FileNameInDir = null;
    private List<String>  VideoFileList = null;
    private Spinner s0;
    private Spinner s1;
    private TextView textview0;
    private TextView textview1;
    private String videoFile0 = "";
    private String videoFile1 = "";
    private final String TAG = "MainActivity";
    private boolean mVideoRunning = false;
    private TextView textView;

        public void onPresentationChanged(boolean change) {
        mPresentation = mHdmiApp.getPresentation();
        Log.i(TAG,"onPresentationChanged:" + change);
        if (change) {
            //create
        }else {
            //dismiss
        }
        }

 
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    textview0 = (TextView)findViewById(R.id.textview0);
    textview1 = (TextView)findViewById(R.id.textview1);
    textview0.setText("SmallScreen");
    textview1.setText("FullScreen");

    File fileDir;
    int fileIndex = 0;
    fileDir = new File("/storage/emulated/legacy/");
    FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".mp4")||name.endsWith(".m4v")||name.endsWith(".avi");
                }
    };
        FileNameInDir = fileDir.list(filter);
        Log.i(TAG,"FileNameInDir.length:" + FileNameInDir.length);
        VideoFileList = new ArrayList<String>();
        for(fileIndex = 0;fileIndex < FileNameInDir.length;fileIndex++){
        VideoFileList.add("/storage/emulated/legacy" + "/" + FileNameInDir[fileIndex]);
        }
        VideoFileList.add("blank");
//        VideoFileList.add("/storage/emulated/legacy" + "/" + FileNameInDir[FileNameInDir.length]);
        
        ArrayAdapter<String> adapter0 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, VideoFileList);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, VideoFileList);
        s0 = (Spinner)findViewById(R.id.spinner0);
        adapter0.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s0.setAdapter(adapter0);
        s1 = (Spinner) findViewById(R.id.spinner1);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s1.setAdapter(adapter1);
        start_button = (Button)findViewById(R.id.start);
        stop_button = (Button)findViewById(R.id.stop);
        start_button.setOnClickListener(MainActivity.this);
        stop_button.setOnClickListener(MainActivity.this);

        mHdmiApp = (HdmiApplication)getApplication();
        mHdmiApp.addListener(this);
        mPresentation = mHdmiApp.getPresentation();
}


@Override
public boolean onCreateOptionsMenu(Menu menu) {
// Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
}


@Override
public void onClick(View v) {
    switch(v.getId())
    {
        case R.id.start:
            int selectIndex0;
            int	selectIndex1;
            selectIndex0 = s0.getSelectedItemPosition();
            selectIndex1 = s1.getSelectedItemPosition();
            if (selectIndex0 == FileNameInDir.length)
            {
                videoFile0 = "blank";
            }else{
                videoFile0 = "/storage/emulated/legacy" + "/" + FileNameInDir[selectIndex0];
            }
        
            if (selectIndex1 == FileNameInDir.length )
            {
                videoFile1 = "blank";
            }else{
                videoFile1 = "/storage/emulated/legacy" + "/" + FileNameInDir[selectIndex1];
            }

            mPresentation.startVideo(videoFile1, videoFile0);
        break;
        case R.id.stop:
            mPresentation.stopVideo();
        break;
        default:
        break;
    }

}

}
