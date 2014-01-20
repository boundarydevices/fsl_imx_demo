/*
 * Copyright (C) 2014 Freescale Semiconductor, Inc.
 */

package com.genius.audio.track;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class AudioTimeStampActivity extends Activity {
    protected static final String REGEX = null;
    /** Called when the activity is first created. */
    final int EVENT_PLAY_OVER = 0x100;
	final int EVENT_PLAY_STOP = 0x200;
	final int EVENT_AVETIME=0x300;
	final int EVENT_MINTIME=0x400;
	final int EVENT_MAXTIME=0x500;
	Thread mThread = null;
	byte []data = null;
	Handler mHandler;
	int frequency,channel,sampbit;
	long my_avetime,my_mintime,my_maxtime;
	Button btnPlay, btnStopButton,btnExit;
	TextView textView;
	TextView resultView1,resultView2,resultView3;
    String  filePath=null;	
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        textView = (TextView)findViewById(R.id.textview01);
        resultView1=(TextView)findViewById(R.id.textview03);
        resultView2=(TextView)findViewById(R.id.textview04);
        resultView3=(TextView)findViewById(R.id.textview05);
        final File[] files = Environment.getExternalStorageDirectory().listFiles();
 
//*******************************files_wav browse********************
        final List<File> fileList = new ArrayList<File>();  		
	    int b=0;
        String wav="wav";
        for(int a=0; a<files.length; a++){
        	int start = files[a].getName().lastIndexOf(".");
        	int end = files[a].getName().length();
    		String indexName = files[a].getName().substring(start + 1, end);
    		if(indexName.equals(wav)){
                fileList.add(files[a]);      			
    		}
    	}
//*********************handler deliver message***********************
    	mHandler = new Handler(){
    	    public void handleMessage(Message message){
    	           if (message.what == EVENT_PLAY_OVER){
	    			   mThread = null;
	    			   textView.setText("Finish...");
	    			   }
    	           else if(message.what ==EVENT_AVETIME){
    	        	   my_avetime=(Long) message.obj;
    	        	 }
    	           else if(message.what ==EVENT_MINTIME){
    	        	   my_mintime=(Long) message.obj;
    	        	  }
    	           else if(message.what ==EVENT_MAXTIME){
    	        	   my_maxtime=(Long) message.obj;
    	        	   resultView1.setText("ave_LatencyTimeMs:          "+my_avetime);
    	    		   resultView2.setText("min_LatencyTimeMs:          "+my_mintime);
    	    		   resultView3.setText("max_LatencyTimeMs:          "+my_maxtime);
    	        	   }
    	        	else if (message.what == EVENT_PLAY_STOP){
	        			mThread = null;
	        		    textView.setText("Stop...");
    	        	    }
    	            }
    	   };
//********************************spinner*****************************
        Spinner spinner = (Spinner)findViewById(R.id.Spinner);
        BaseAdapter ba = new BaseAdapter()
		{
			@Override
			public int getCount()
			{
				return fileList.size();
			}

			@Override
			public Object getItem(int position)
			{
				return null;
			}

			@Override
			public long getItemId(int position)
			{
				// TODO Auto-generated method stub
				return 0;
			}
	    	@Override
			public View getView(int position, View convertView, ViewGroup parent) 
			{
				LinearLayout line = new LinearLayout(AudioTimeStampActivity.this);
				line.setOrientation(0);
				ImageView image = new ImageView(AudioTimeStampActivity.this);
				image.setImageResource(R.drawable.icon);
				TextView text = new TextView(AudioTimeStampActivity.this);
				text.setText(fileList.get(position).getName());
				text.setTextSize(30);
				text.setTextColor(R.color.blue);
				line.addView(image);
				line.addView(text);
				return line;
			}		
		};
	   	spinner.setAdapter(ba);
	    spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
			filePath="/sdcard/"+fileList.get(arg2).getName();
			// TODO Auto-generated method stub
		    if(filePath!=null){
		     data=null;
			 data = getPCMData();
             init();
		     }
		 }
            @Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		              }
                }
	    );
  }
        public byte[] getPCMData(){
    	File file = new File(filePath);
    	if (file == null|file.length()<=44){
    		  return null;
    	   }
    	FileInputStream inStream;
		try {
			inStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		byte[] data_pack = null;
    	if (inStream != null){
    		long size = file.length()-44;
    		data_pack = new byte[(int) size];
    		try {
                byte[] tmp = new byte[4];
                System.out.println(""+tmp.length);
                byte[] tmp2= new byte[2];
//************************RIFF*******************************
    		    inStream.read(tmp);
    		    String id = new String(tmp);
    			
//***********the total numbers of bytes from the next address to the file tail*************    			
    			inStream.read(tmp);
    			int value2 = 0;
    			int temp2=0;
    			 for (int i = 3; i >= 0; i--) {
    				 value2<<=8;
    				 temp2=tmp[i]&0xff;
    				 value2|=temp2;
    				        }
               
//************************WAVE******************************
    			inStream.read(tmp);
    			String type3 = new String(tmp);
    			System.out.println("type3="+type3);
//************************fmt*******************************    			
    			inStream.read(tmp);
    			String type4 = new String(tmp);
    			System.out.println("type4="+type4);
//****************filtering bytes(generally 00000010h)************************
    			inStream.read(tmp);
    	        int value5 = 0;
    			int temp5=0;
   			    for (int i = 3; i >=0; i--) {
	   			    	value5<<=8;
	   				    temp5=tmp[i]&0xff;
	   				    value5|=temp5;
   				        }
    		   
//****************format type(if the value is 1, it means PCM)********************
    			inStream.read(tmp2);
        		int value6 = 0;
    			int temp6=0;
   			    for (int i = 1; i >=0; i--) {
	   				 value6<<=8;
	   				 temp6=tmp2[i]&0xff;
	   				 value6|=temp6;
   				        }
    			
//*************************Channel*****************************
    			inStream.read(tmp2);
    			int value7= 0;
    			int temp7;
   			    for (int i = 1; i >=0; i--) {
   				 value7<<=8;
   				 temp7=tmp2[i]&0xff;
   				 value7|=temp7;
   				}
    	    	channel=value7;
    			
//************************frequency*******************************
    			inStream.read(tmp);
    			int value8= 0;
    			int temp8;
   			    for (int i = 3; i >=0; i--) {
	   				 value8<<=8;
	   				 temp8=tmp[i]&0xff;
	   				 value8|=temp8;
   				}
                frequency=value8;
    			
//************************data transfer rate****************************
    			inStream.read(tmp);
    			int value9= 0;
    			int temp9;
   			    for (int i = 3; i >=0; i--) {
	   				value9<<=8;
	   				temp9=tmp[i]&0xff;
	   				value9|=temp9;
   				}
    			
//************************the length of data block ************************
    			inStream.read(tmp2);
    			int value10= 0;
    			int temp10;
   			    for (int i = 1; i >=0; i--) {
	   				 value10<<=8;
	   				 temp10=tmp2[i]&0xff;
	   				 value10|=temp10;
   				}
               
//************************sample bits*******************************
    			inStream.read(tmp2);
    			int value11= 0;
    			int temp11;
   			    for (int i = 1; i >=0; i--) {
	   				 value11<<=8;
	   				 temp11=tmp2[i]&0xff;
	   				 value11|=temp11;
   				}
   			    sampbit=value11/8;
    			
//************************fact*******************************
    			inStream.read(tmp);
    			int value12= 0;
    			int temp12;
   			    for (int i = 3; i >=0; i--) {
	   				 value12<<=8;
	   				 temp12=tmp[i]&0xff;
	   				 value12|=temp12;
   				}
  			   
//************************size, the value is 4**************************
    			inStream.read(tmp);
    			int value13= 0;
    			int temp13;
   			    for (int i = 3; i >=0; i--) {
	   				 value13<<=8;
	   				 temp13=tmp[i]&0xff;
	   				 value13|=temp13;
   				}
        	
//************************data area******************************
        		inStream.read(data_pack);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
         }
    	
    	return data_pack;
    }
    
    public void play(){
    	
    	resultView1.setText("");
        resultView2.setText("");
        resultView3.setText("");
    	if (data == null){
    		Toast.makeText(this, "No File...", 200).show();
    		return ;
    	}
    	
    	if (mThread == null){
    		mThread = new Thread(new AudioPlayThread(data, mHandler,frequency,channel,sampbit));
    		System.out.println("*************frequency  "+frequency+"   channel   "+channel+"   sampbit   "+sampbit);
    		System.out.println("------------my_avetime-------  "+my_avetime+"---my_mintime-----"+my_mintime+"----my_maxtime---"+my_maxtime);
    		mThread.start();
    		textView.setText("Playing...");
    	}
 }
    
    public void stop(){
    	if (data == null){
    		return ;
    	}
        if (mThread != null){
    		mThread.interrupt();
    		mThread = null;
    	}
     if(my_avetime==0)resultView1.setText("");
        else  resultView1.setText("ave_LatencyTimeMs:          "+my_avetime);
              resultView2.setText("min_LatencyTimeMs:          "+my_mintime);
              resultView3.setText("max_LatencyTimeMs:          "+my_maxtime);
       }
    public void init(){
    	btnPlay = (Button)findViewById(R.id.buttonPlay);
    	btnStopButton = (Button)findViewById(R.id.buttonStop);
    	btnPlay.setOnClickListener(new OnClickListener() {
		public void onClick(View v) {
				// TODO Auto-generated method stub
				play();
	          }
		});
        btnStopButton.setOnClickListener(new OnClickListener() {
		public void onClick(View v) {
				// TODO Auto-generated method stub
				stop();
		}
		});
 }

    public void onDestroy(){
    	System.exit(0);
    	super.onDestroy();
    }
}
