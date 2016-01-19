/*
 * Copyright (C) 2016 Freescale Semiconductor, Inc.
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
package com.freescale.bleclient.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ListViewUtil {
	
	public static void setListViewHeight(ListView listView) {    


		ListAdapter listAdapter = listView.getAdapter();    

		if (listAdapter == null) {    
			return;    
		}    
		int totalHeight = 0;    
		for (int i = 0, len = listAdapter.getCount(); i < len; i++) { 
			View listItem = listAdapter.getView(i, null, listView);    
			listItem.measure(0, 0); 
			totalHeight += listItem.getMeasuredHeight(); 
		}    

		ViewGroup.LayoutParams params = listView.getLayoutParams();    
		params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));    
		listView.setLayoutParams(params);    
	} 
}
