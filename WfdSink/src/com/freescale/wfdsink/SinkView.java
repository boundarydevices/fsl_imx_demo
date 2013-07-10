/*
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

package com.freescale.wfdsink;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.WindowManager;
import android.view.Display;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.net.wifi.p2p.WifiP2pDevice;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.util.AttributeSet;

public class SinkView extends SurfaceView {
    //define the TS stream resolution.
    private int sinkW = 1280;
    private int sinkH = 720;

    public SinkView(Context context) {
        super(context);
    }

    public SinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SinkView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        if (previewWidth > previewHeight * sinkW / sinkH) {
            previewWidth = previewHeight * sinkW / sinkH;
        }
        else {
            previewHeight = previewWidth * sinkH /sinkW;
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
    }
}

