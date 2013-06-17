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

public class ItemData {
	public String mTitle;
	public String mName;
	public String mPath;
	public String mMime;
	public long   mDuration;
	public String mArt;
	public String mBucket;

	public ItemData() {
		mTitle = null;
		mName  = null;
		mPath  = null;
		mMime  = null;
		mDuration = 0;
		mArt   = null;
		mBucket = null;
	}

}

