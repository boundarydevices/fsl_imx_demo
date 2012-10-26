/*
 * Copyright (C) 2012 Freescale Semiconductor, Inc.
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

package fsl.power.service;

import android.net.Uri;
import android.provider.BaseColumns;

public final class PowerServiceDB {

	public PowerServiceDB() {
		// TODO Auto-generated constructor stub
	}
	public static final String AUTHORITY = "fsl.power.service.PowerServiceDB";

	/*
	 * Profile table
	 */
	public static final class Profiles implements BaseColumns {
		private Profiles() {}
		/*
		 *  The content:// style URL for this table
		 */
       public static final Uri CONTENT_URI
						= Uri.parse("content://" + AUTHORITY + "/profiles");

       public static final Uri CONTENT_URI_ID
								= Uri.parse("content://" + AUTHORITY + "/profiles/#");
       /**
	* The MIME type of {@link #CONTENT_URI} providing a directory of profiles.
	*/
       public static final String CONTENT_TYPE
								= "vnd.android.cursor.dir/vnd.power.service";

       /**
	* The MIME type of a {@link #CONTENT_URI} sub-directory of a single profile.
	*/
       public static final String CONTENT_ITEM_TYPE
								= "vnd.android.cursor.item/vnd.power.service";

       /**
	* The default sort order for this table
	*/
       public static final String DEFAULT_SORT_ORDER = "name DESC";

       /**
	* The ID of the profiles
	This param is not used in this version,and we use the "_ID" instead.
	* <P>Type: integer</P>
	*/
       public static final String ProfileID = "profileID";

       /**
	* The name of profile
	* There are 3 prefix profile for users to select:
		*  Performance Mode
		*  Power Saving Mode
		*  WebBrowsering Mode
	* <P>Type: TEXT</P>
	*/
       public static final String ProfileName = "name";
       public static final String PfofileStatus = "status";
       public static final String TempHot = "hot";
       public static final String TempActive = "active";
       public static final String MaxFreq = "maxfreq";
       public static final String MinFreq = "minfreq";
       public static final String CurCPUGov = "governor";
       public static final String CPUHotPlug = "cpuhotplug";
       public static final String CPUNM = "cpunm";

	}

}
