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

import android.content.Intent;
import android.content.Context;

import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

public class ImageTextView extends LinearLayout {
	private static final String CLASS = "ImageTextView: ";

	// access to owner activity
	private Context mContext;

	// directory or file
	private boolean mDir;

	// if directory, then have an ID attached
	private int     mDirId;

	private ImageView mThumbnailImage;
	private TextView  mNameView; // directory or file name
	private TextView  mDurationView;

	private String mName;
	private String mUrl;
	private String mAlbumArt;
	private long   mDuration;

	// task for displaying photo thumbnails
	private DisplayPicture mThumbNailTask = null;

	public ImageTextView(Context context) {
		super(context);

		mContext = context;

		mThumbNailTask = null;

		LayoutInflater inflater =
			(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		inflater.inflate(R.layout.griditemextab, this, true);

		// findViewById's performance is low

		mThumbnailImage = (ImageView)findViewById(R.id.grid_item_big_image);
		mDurationView   = (TextView) findViewById(R.id.grid_item_duration);
		mNameView      = (TextView) findViewById(R.id.grid_item_name);

		clear();
	}

	public void onFileClicked() {
		if(!mDir)
			SendToLocalRenderer(mUrl);
	}

	public boolean isDir() {
		return mDir;
	}

	public int dirId() {
		if(!isDir())
			return -1;
		return mDirId;
	}

	private void SendToLocalRenderer(String url) {
		if(url == null)
			return;

		Intent intent;
		intent = new Intent(mContext, VideoPlayer.class);
		intent.setData(Uri.parse(url));
		mContext.startActivity(intent);
	}

	public void clear() {
		// cancel previous tasks
		if(mThumbNailTask != null)
		{
			mThumbNailTask.cancel(true);
			mThumbNailTask.setCancelled();
		}

		mName     = "Bad file";
		mUrl      = null;
		mDuration = 0;
		mAlbumArt = null;
		mDir      = false;
		mDirId    = -1;

		mNameView     .setText(mName);
		mDurationView  .setText(null);
		mThumbnailImage.setImageDrawable(null);
	}

	public void update(String dirName, int childCount, int id) {
		// cancel previous tasks
		if(mThumbNailTask != null)
		{
			mThumbNailTask.cancel(true);
			mThumbNailTask.setCancelled();
		}

		mName = dirName;
		mUrl   = null;
		mDuration = 0;
		mAlbumArt = null;
		mDir   = true;
		mDirId = id;

		mNameView.setText(mName);
		mDurationView.setText(null);
		mThumbnailImage.setImageResource(R.drawable.folder);
		if(mAlbumArt != null)
		{
			Log.d(CLASS, "Set art " + mAlbumArt);
			setImage(mAlbumArt);
		}
	}

	public void update(ItemData data) {
		// cancel previous tasks
		if(mThumbNailTask != null)
		{
			mThumbNailTask.cancel(true);
			mThumbNailTask.setCancelled();
		}

		if(data == null)
			return;

		mName  = data.mName;
		mUrl   = data.mPath;
		mDuration = data.mDuration;
		mAlbumArt = data.mArt;
		mDir   = false;
		mDirId = -1;

		mNameView.setText(mName);
		long seconds = mDuration / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		minutes = minutes % 60;
		seconds = seconds % 60;
		String fmtDuration = hours + ":";
		if(minutes < 10)
			fmtDuration += "0";
		fmtDuration += minutes + ":";
		if(seconds < 10)
			fmtDuration += "0";
		fmtDuration += seconds;
		mDurationView.setText(fmtDuration);
		mThumbnailImage.setImageResource(R.drawable.file);
		if(mAlbumArt != null)
		{
			Log.d(CLASS, "Set art " + mAlbumArt);
			setImage(mAlbumArt);
		}
	}

	private void setImage(String file) {
		// cancel previous tasks
		if(mThumbNailTask != null)
		{
			mThumbNailTask.cancel(true);
			mThumbNailTask.setCancelled();
		}

		mThumbNailTask = new DisplayPicture();
		try {
			mThumbNailTask.execute(file);
		}
		catch(IllegalStateException e) {
			Log.d(CLASS, "image decoded failed or cancelled");
		}
	}

	// non-static inner class: will access enclosing class instance's non-static member
	private  class DisplayPicture extends AsyncTask<String, Integer, Bitmap> {
		private boolean mCancelled = false;

		protected Bitmap doInBackground(String... sUrl) {
			mCancelled = false;
			Bitmap bitmap = BitmapFactory.decodeFile(sUrl[0]);
			return bitmap;
		}

		public void setCancelled() {
			mCancelled = true;
		}

		@Override
		protected void onCancelled() {
			//Log.d(TAG, "image decoded cancelled");
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if(mCancelled == false && result != null) {
				mThumbnailImage.setImageBitmap(result);
			}
			else {
				//Log.d(TAG, "image decoded failed or cancelled");
			}
		}
	}

}
