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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.util.Log;


// display small icon and text
public class ImageTextAdapter extends BaseAdapter {
	private Context mContext = null;

	private Bucket mBuckets[] = null;
	private int mBucketCount = 0;

	private int mCurrentBucket = -1; // -1: root

	public ImageTextAdapter(Context c) {
		mContext = c;

		mBuckets = new Bucket [4];
		int i;
		for(i=0; i<mBuckets.length; i++)
			mBuckets[i] = new Bucket();

		mBucketCount = 0;
		mCurrentBucket = -1;
	}

	synchronized public int getCount() {
		if(mCurrentBucket == -1)
			return mBucketCount;
		else
			return mBuckets[mCurrentBucket].length();
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	synchronized public View getView(int position, View convertView, ViewGroup parent) {
		ImageTextView itv = (ImageTextView) convertView;
		if (itv == null)
			itv = new ImageTextView(mContext);

		if(mCurrentBucket == -1 && position >= 0 && position < mBucketCount)
			itv.update(mBuckets[position].name(), mBuckets[position].length(), position);
		else if(mCurrentBucket >= 0 && mCurrentBucket < mBucketCount && position >= 0 && position < mBuckets[mCurrentBucket].length())
			itv.update(mBuckets[mCurrentBucket].item(position));
		else
			itv.clear();

		return itv;
	}

	private void createBuckets(ItemData[] data, int count) {
		int i;
		for(i=0; i<count; i++) {
			String bucketName = data[i].mBucket;

			// find a bucket with bucketName
			int j;
			for(j=0; j<mBucketCount; j++) {
				if(mBuckets[j].name().equals(bucketName))
					break;
			}

			// check bucket array size
			if(j >= mBuckets.length) {
				// assert(j == mBucketCount)
				Bucket [] newBuckets = new Bucket [(mBuckets.length+1) * 2];
				int k;
				for(k=0; k<mBucketCount; k++) {
					newBuckets[k] = mBuckets[k];
				}
				for(; k<newBuckets.length; k++)
					newBuckets[k] = new Bucket();

				mBuckets = newBuckets;
			}

			mBuckets[j].insert(data[i]);

			if(j == mBucketCount)
				mBucketCount++;
		}
	}

	synchronized public void setData(ItemData[] data, int count) {
		createBuckets(data, count);
		mCurrentBucket = -1;

		notifyDataSetChanged();
	}

	public void clickItem(ImageTextView itv) {
		if(itv == null)
			return;

		if(itv.isDir()) {
			mCurrentBucket = itv.dirId();
			notifyDataSetChanged();
		}
		else {
			itv.onFileClicked();
		}
	}

	public boolean onBackPressed() {
		if(mCurrentBucket == -1) {
			return false;
		}
		else {
			mCurrentBucket = -1;
			notifyDataSetChanged();
			return true;
		}
	}

	private class Bucket {
		private String mName;
		private ItemData [] mData = null;
		private int mItemCount = 0;

		public Bucket() {
			mName = null;
			mData = new ItemData [4];
			mItemCount = 0;
		}

		public void insert(ItemData data) {
			// if this is the first item, then get its bucket name
			if(mName == null)
				mName = data.mBucket;

			// if overflow
			if(mItemCount + 1  == mData.length) {
				ItemData [] newData = new ItemData [(mData.length+1) * 2];
				int i;
				for(i=0; i<mItemCount; i++) {
					newData[i] = mData[i];
				}
				mData = newData;
			}

			// insert the item
			mData[mItemCount] = data;
			mItemCount++;
		}

		public String name() {
			return mName;
		}

		public int length() {
			return mItemCount;
		}

		public ItemData item(int pos) {
			if(pos < 0 || pos >= length())
				return null;
			else
				return mData[pos];
		}
	}
}

