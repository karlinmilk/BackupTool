package com.android.backuptool;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.android.backuptool.utils.BackupValue;
import com.android.backuptool.utils.Logger;
import com.android.backuptool.utils.Utility;
import com.internal.library.os.SystemProperties;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class BackupService extends Service {
	public static final String LOG_TAG = "BackupService";
	
	public static final String ALARM_ACTION = "com.android.BackupTool.ALARM_ALERT";

	private final int ZIP_FILE = 0;
	
	//--setting value--
	private String mFtpAddr;	
	private String mFtpUser;
	private String mFtpPassword;
	private String mFtpULPath;
	private int mBackupPeriod;
	private int mTimeout;
	
	//--get SN value
	public String mDeviceSN;
	
	//--service running status
	public static boolean mServiceRun=false;
	
	//--check if SR is paused flag--
	private String mPaused;
	//--check if SR is stoped flag--
	public boolean mSRstopped;
	//--check if SR is paused within timeout--
	private int mWaitCount;
	//--zip file name--
	private String mZipFileName;
	//--zip file path--
	private String mZipFile;
	
	//--alarm name--
	private final String CHECKPROP_RUN_ALARM = "checkprop_run_alarm";		
	//--alarm type--
	private final int ALARM_CHECKPROP = 1;	
	
	//store bugreport file name
	private String mBugFile;
	
	//store upload record file name
	private String mRecordFile;
	
	//upload really finish flag
	private boolean mULfinish=false;
	
	private  Handler mErrorHandler;

	public final Handler mMessageHandler = new Handler() {
		public synchronized void handleMessage(Message msg) {
			if(msg.what == SFTPProcess.UPLOAD_FINISH){
				/**
				 * 1.上傳<Task_ID>_<Job_ID>_<yyyyMMddHHmmss>.txt 表示完成 
				 * ~/SFTPATSOutput/UploadRecord/<Task_ID>_<Job_ID>_<yyyyMMddHHmmss>.txt
				 * 2.disconnect ftp				
				 */
				if(!mULfinish)
				{
					BackupLogger.logging(LOG_TAG,"upload record file");
					writeUploadRecordFile();
					SFTPProcess.mFTPULWorkingDir = "SFTPATSOutput/UploadRecord";
					SFTPProcess.mFTPULFile = mRecordFile;
					SFTPProcess process=new SFTPProcess();
					process.startUpload();
				}
				else
				{				
					BackupLogger.logging(LOG_TAG,"upload finish----");
					setToast("upload finish......");
					SFTPProcess ftpproc=new SFTPProcess();
					ftpproc.startDisconnect();
				}

			}
			else if(msg.what == SFTPProcess.FTP_DISCONNECTED)
			{
				BackupLogger.logging(LOG_TAG,"SFTP disconnected......");
				setToast("Sftp disconnected......");
				/**
				 * 3.delete log file and zip file on device
				 * 4.Start htclog logging
				 * 5.Resume SR
				 */
				
				deleteAllLog();
				startHtcLogging();      		
        		resumeSR();
        		//--set next backup alarm--
        		//假設backup一半,插上usb或是disable tool(會進到onDestroy把mServiceRun設成false),就不要再設下一次的alarm
        		//如果SR is stopped,也不要再設下一次的alarm
        		BackupLogger.logging(LOG_TAG,"service running:"+mServiceRun);
        		if(mServiceRun && !mSRstopped)
        		{       			
        			BackupReceiver.setAlarm(BackupService.this, BackupReceiver.BACKUP_RUN_ALARM, 
        				mBackupPeriod*60*1000, BackupReceiver.ALARM_BACKUP);
        		}
        		//stop service 避免影響user run script,若有設backup alarm,並不會cancel
        		stopSelf();
        		
			}
			else if(msg.what == ZIP_FILE)
			{
				/**
        		 * set zip path and zip name(by time)
        		 */
				setToast("zip start...");
				BackupLogger.logging(LOG_TAG,"zip file......");
				String time=(String) msg.obj;
				String zipfilepath="/sdcard";

				BackupReceiver.HourBackupId=BackupReceiver.HourBackupId+1;
				BackupLogger.logging(LOG_TAG, "HourBackupId:"+BackupReceiver.HourBackupId);

				mZipFileName="SST_"+BackupReceiver.GroupId+"_"+BackupReceiver.HourBackupId+".zip";
        		mZipFile=zipfilepath+"/"+mZipFileName;
        		zipAllLog(mZipFile); 
        		SFTPProcess.mFTPULWorkingDir = "SFTPATSOutput/"+BackupReceiver.TaskId+"/"+BackupReceiver.JobId+"/"+mDeviceSN;
        		SFTPProcess.mFTPULFile=mZipFile;
        		
        		//--connect ftp->upload--
        		setToast("Start FTP Process......");
        		SFTPProcess process=new SFTPProcess();
        		process.startProcess();    
			}
		}
	};
	

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		BackupLogger.logging(LOG_TAG,"on Create BackupService");
		//readATSTestInfo();
		super.onCreate();
		IntentFilter intentFilter=new IntentFilter(ALARM_ACTION);
		registerReceiver(mReceiver, intentFilter);	
		

	}
	
	@Override
	@Deprecated
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		BackupLogger.logging(LOG_TAG,"+++ on Start BackupService +++");
		super.onStart(intent, startId);
		mServiceRun=true;
		readBackupSetting();
		mDeviceSN=SystemProperties.get("ro.boot.serialno");
		BackupLogger.logging(LOG_TAG, "SN:"+mDeviceSN);

		mErrorHandler = new Handler() {
			public synchronized void handleMessage(Message msg) {
				switch (msg.what) {
				case SFTPProcess.Status_MSG:
					Toast.makeText(BackupService.this,
							"Data connection is not ready!!!", Toast.LENGTH_SHORT)
							.show();
					BackupLogger.logging(LOG_TAG,"Data connection is not ready!!!");

					/**
					 * retry connect
					 */
					if(mServiceRun)
					{
						SFTPProcess ftpproc=new SFTPProcess();
						ftpproc.startProcess();
					}
					else
						return;
					break;
				case SFTPProcess.Upload_MSG:
					//String method = (String) msg.obj;
					Toast.makeText(BackupService.this,
							"Upload fail!!!", Toast.LENGTH_SHORT)
							.show();
					BackupLogger.logging(LOG_TAG,"Upload fail!!!");

					/**
					 * retry upload
					 */
					if(mServiceRun)
					{
						SFTPProcess ftpproc1=new SFTPProcess();
						ftpproc1.startUpload();
					}
					else
						return;
					break;
				}

			}
		};
		
		
		SFTPProcess.mErrorHandler=mErrorHandler;
		SFTPProcess.mMessageHandler=mMessageHandler;
		SFTPProcess.mFTPAddress=mFtpAddr;
		SFTPProcess.mFTPUser=mFtpUser;
		SFTPProcess.mFTPPassword=mFtpPassword;
		//[note]要改default的路徑
		SFTPProcess.mFTPULDefaultPath=mFtpULPath;

		mWaitCount=0;
	
		pauseSR();
		
		//[note] 要改成每一分鐘
		//check if SR is paused every 1 sec
		setAlarm(CHECKPROP_RUN_ALARM,60*1000,ALARM_CHECKPROP);
   	 	//setAlarm(CHECKPROP_RUN_ALARM,1000,ALARM_CHECKPROP);
	}
	
	private void readBackupSetting() {
		SharedPreferences sp = getSharedPreferences("BackupTool",
				MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
		mFtpAddr=sp.getString(BackupValue.FTP_ADDR, BackupValue.DEFAULT_FTP_ADDR);
		mFtpUser=sp.getString(BackupValue.FTP_USERNAME, BackupValue.DEFAULT_FTP_USERNAME);
		mFtpPassword=sp.getString(BackupValue.FTP_PASSWORD, BackupValue.DEFAULT_FTP_PASSWORD);
		mFtpULPath=sp.getString(BackupValue.FTP_UL_PATH, BackupValue.DEFAULT_FTP_UL_PATH);
		mBackupPeriod=Integer.parseInt(sp.getString(BackupValue.BACKUP_PERIOD, BackupValue.DEFAULT_BACKUP_PERIOD));
		mTimeout=Integer.parseInt(sp.getString(BackupValue.TIMEOUT, BackupValue.DEFAULT_TIMEOUT));
		BackupLogger.logging(LOG_TAG,"mFTPUser:"+mFtpUser+",mFtpPassword:"+mFtpPassword);
		BackupLogger.logging(LOG_TAG,"mFtpAddr:"+mFtpAddr+",mFtpULPath:"+mFtpULPath+",mBackupPeriod:"+mBackupPeriod+",mTimeout:"+mTimeout);
	}				
	
	public void setAlarm(String alarm, long duration, int type) {
		BackupLogger.logging(LOG_TAG,"setAlarm");
		long currentTime = System.currentTimeMillis();
		long alarmTime = currentTime + duration;
		BackupLogger.logging(LOG_TAG,"current time:"+Utility.convertMillisToDate(currentTime,"yyyy/MM/dd-HH:mm:ss"));
		BackupLogger.logging(LOG_TAG,"alarm time:"+Utility.convertMillisToDate(alarmTime,"yyyy/MM/dd-HH:mm:ss"));
		try {
			Object service = getSystemService(Context.ALARM_SERVICE);
			AlarmManager alarmManager = (AlarmManager) service;

			Intent intent = new Intent(ALARM_ACTION);
			intent.putExtra("alarm_type", type);

			PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);
			alarmManager.cancel(sender);
			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);
		} catch (Exception e1) {
			//Log.e(LOG_TAG, "Exception: " + e1.getMessage());
			BackupLogger.logging(LOG_TAG, "setAlarm Exception-", e1);
		}
	}
	
	protected void onAlarm(int alarm_type) {
		switch (alarm_type) {
			case ALARM_CHECKPROP:
				
				BackupLogger.logging(LOG_TAG,"Run ALARM_CHECKPROP!!!");
				long currentTime1 = System.currentTimeMillis();
				final String time=Utility.convertMillisToDate(currentTime1,"yyyyMMdd_HHmmss");
				BackupLogger.logging(LOG_TAG,"current time:"+time);
				
				//--Check if SR is stopped
				mSRstopped = checkSRFinished();
				//--Check if SR is paused--
				mPaused=SystemProperties.get("mr.paused");
				BackupLogger.logging(LOG_TAG,"mPaused:"+mPaused);
				BackupLogger.logging(LOG_TAG,"mWaitCount:"+mWaitCount);
            	//------------------------------------
				if(!mSRstopped)
				{
	            	if(mPaused.equalsIgnoreCase("true"))
	            	{
	            		//--stop htc logging--
	            		stopHtcLogging();	            		
	            		new Thread()
	            	    {
	            			public void run()
	            	        {
	            			     try
	            			     {
	            			    	Log.i(LOG_TAG,"add report");
	            			    		
	            			        addBugReport();
	            			        mMessageHandler.obtainMessage(ZIP_FILE, time).sendToTarget();
	            			     }
	            			     catch (Exception e)
	            			     {
	            			        // TODO Auto-generated catch block
	            			        e.printStackTrace();
	            			        Log.e(LOG_TAG,"exception:"+e);
	            			     }
	            	        }
	            	    }.start();
	            		            	    
	            	}
	            	else if(mPaused.equalsIgnoreCase("false") && mWaitCount<40)
	            	{
	            		//[note] 要改成一分鐘
	            		setAlarm(CHECKPROP_RUN_ALARM,60*1000,ALARM_CHECKPROP);
	            		//setAlarm(CHECKPROP_RUN_ALARM,1000,ALARM_CHECKPROP);
	            		mWaitCount++;
	            	}
	            	else if(mPaused.equalsIgnoreCase("false") && mWaitCount>=40)
	            	{
	            		//--Check if SR is stopped
	    				mSRstopped = checkSRFinished();
	            		stopHtcLogging();      		          		
	            		new Thread()
	            	    {
	            			public void run()
	            	        {
	            			     try
	            			     {
	            			    	Log.i(LOG_TAG,"add report");
	            			    		
	            			        addBugReport();
	            			        mMessageHandler.obtainMessage(ZIP_FILE, time).sendToTarget();
	            			     }
	            			     catch (Exception e)
	            			     {
	            			        // TODO Auto-generated catch block
	            			        e.printStackTrace();
	            			        Log.e(LOG_TAG,"exception:"+e);
	            			     }
	            	        }
	            	    }.start();            		
	            	}
				}
				else
				{
					//If SR is stopped,zip log and upload
					stopHtcLogging();      		          		
            		new Thread()
            	    {
            			public void run()
            	        {
            			     try
            			     {
            			    	 BackupLogger.logging(LOG_TAG,"add report");
            			    		
            			        addBugReport();
            			        mMessageHandler.obtainMessage(ZIP_FILE, time).sendToTarget();
            			     }
            			     catch (Exception e)
            			     {
            			        // TODO Auto-generated catch block
            			        e.printStackTrace();
            			        BackupLogger.logging(LOG_TAG,"addBugReport exception-",e);
            			     }
            	        }
            	    }.start();            		
            	
				}
			break;
				
		}
	}
	
	public void cancelAlarm(String alarm, int type) {
		Object service = getSystemService(Context.ALARM_SERVICE);
		AlarmManager alarmManager = (AlarmManager) service;
		try {
			Intent intent = new Intent(ALARM_ACTION);
			intent.putExtra("alarm_type", type);

			PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			alarmManager.cancel(sender);
		} catch (Exception e1) {
			//Log.e(LOG_TAG, "Exception: " + e1.getMessage());
			BackupLogger.logging(LOG_TAG, "cancel Alarm Exception-", e1);
		}
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context arg0, Intent intent) {
			BackupLogger.logging(LOG_TAG,"intent ="+intent.getAction());
			if (intent.getAction().equals(ALARM_ACTION)){
				int alarm_type = intent.getIntExtra("alarm_type", -1);
				onAlarm(alarm_type);
			}
		}
	};
	
	private boolean checkDirOrFileExists(String path)
	{
		File f=new File(path);
		if(f.exists())
			return true;
		return false;
	}

	private void changeFileMode(String FileName)
	{
		String cmd = "/system/xbin/su 0 chmod -R 777 "+FileName;
		try
        {
            Process proc=Runtime.getRuntime().exec(cmd);
            /*
            InputStream error = proc.getErrorStream();
            for (int i = 0; i < error.available(); i++) {
               Log.i(LOG_TAG,"exec:" + error.read());
            }
            */


        }
        catch (IOException e1)
        {
            // TODO Auto-generated catch block
        	BackupLogger.logging(LOG_TAG, "changeFileMode "+cmd+" fail-", e1);
        }
        Log.i(LOG_TAG, "cmd:" + cmd);
        try
        {
            Thread.sleep(700);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        cmd = "/system/xbin/su 0 chmod -R 777 "+FileName+"/*";
        try
        {
        	Process proc=Runtime.getRuntime().exec(cmd);
        	InputStream error = proc.getErrorStream();
            for (int i = 0; i < error.available(); i++) {
               Log.i(LOG_TAG,"exec:" + error.read());
            }
        }
        catch (IOException e1)
        {
            // TODO Auto-generated catch block
        	BackupLogger.logging(LOG_TAG, "changeFileMode "+cmd+" fail-", e1);
        }
        Log.i(LOG_TAG, "cmd:" + cmd);
        try
        {
            Thread.sleep(700);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
	}
	
	public ZipOutputStream zip(String inputFileName, String zipFileName, ZipOutputStream output,String foldername) throws Exception {  
		BackupLogger.logging(LOG_TAG,"local file name:"+inputFileName);  
        File inputFile =new File(inputFileName);
        if(output==null)
		{
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
			output=out;
		}
        output.setLevel(1);
        if(inputFile.isDirectory())
        {
	        File[] fl = inputFile.listFiles();
	        if(fl==null)
            {
            	Log.i(LOG_TAG,inputFileName+"is null");
            	zip(output,null,foldername);
            }
            else
            {
            	Log.i(LOG_TAG,"length:"+fl.length);
            	if(fl.length==0)
            		zip(output,null,foldername);
            	else
            	{
			        for (int i = 0; i < fl.length; i++) {  
			           zip(output, fl[i], fl[i].getName(),foldername);  
			        }
            	}
            }
        }
        else
        	zip(output, inputFile, inputFile.getName(),foldername);  
          
        BackupLogger.logging(LOG_TAG,"zip done");  
        //out.close();
        return output;
        
        
    }  
	
	//for rename filename in zip
	public ZipOutputStream zip(String inputFileName, String zipFileName, ZipOutputStream output,String foldername, String rename) throws Exception {  
		BackupLogger.logging(LOG_TAG,"local file name:"+inputFileName);
        File inputFile =new File(inputFileName);
        if(output==null)
		{
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
			output=out;
		}
        output.setLevel(1);
        if(inputFile.isDirectory())
        {
	        File[] fl = inputFile.listFiles();
	        if(fl==null)
            {
            	Log.i(LOG_TAG,inputFileName+"is null");
            	zip(output,null,foldername);
            }
            else
            {
            	Log.i(LOG_TAG,"length:"+fl.length);
            	if(fl.length==0)
            		zip(output,null,foldername);
            	else
            	{
			        for (int i = 0; i < fl.length; i++) {  
			           zip(output, fl[i], fl[i].getName(),foldername);  
			        }
            	}
            }
        }
        else
        	zip(output, inputFile, inputFile.getName(),foldername,rename);  
          
        BackupLogger.logging(LOG_TAG,"zip done");  
        //out.close();
        return output;
        
        
    }  
	
	//zip 特定副檔名的檔案
	public ZipOutputStream zipOption(String inputFileName, String zipFileName, ZipOutputStream output,String foldername,String extension) throws Exception {  
		BackupLogger.logging(LOG_TAG,"local file name:"+inputFileName); 
        File inputFile =new File(inputFileName);
        if(output==null)
		{
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
			output=out;
		}
        output.setLevel(1);
        if(inputFile.isDirectory())
        {
	        File[] fl = inputFile.listFiles();
	        if(fl==null)
            {
	        	BackupLogger.logging(LOG_TAG,inputFileName+"is null");
            	zip(output,null,foldername);
            }
            else
            {
            	BackupLogger.logging(LOG_TAG,"length:"+fl.length);
            	if(fl.length==0)
            		zip(output,null,foldername);
            	else
            	{
            		zip(output,null,foldername); //避免如果沒有任何屬於extension的file,就不會create a empty folder in zip file
			        for (int i = 0; i < fl.length; i++) {  
			        	zipOption(output, fl[i], fl[i].getName(),foldername,extension);   
			        }
            	}
            }
        }
         
        BackupLogger.logging(LOG_TAG,"zip done");  
        //out.close();
        return output;
        
        
    }  

	/*
	private ZipOutputStream zip(String zipFileName, File inputFile,ZipOutputStream output ) throws Exception {
		if(output==null)
		{
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
			output=out;
		}
        output.setLevel(1);
        zip(output, inputFile, "");  
        Log.i(LOG_TAG,"zip done");  
        //out.close();
        return output;
    }*/
	
	private void zip(ZipOutputStream out, File f, String filename, String foldername) throws Exception {  
        if (f.isDirectory()) {  
        	BackupLogger.logging(LOG_TAG,f.getName()+" is dir");
            File[] fl = f.listFiles();  
            if(fl==null)
            {
            	BackupLogger.logging(LOG_TAG,filename+"is null");
            	zip(out,filename+"/",foldername);
            }
            else
            {
            	BackupLogger.logging(LOG_TAG,"length:"+fl.length);
            	if(fl.length==0)
            		zip(out,filename+"/",foldername);
            	else
            	{
		            for (int i = 0; i < fl.length; i++) {  
		                zip(out, fl[i], filename +"\\" + fl[i].getName(),foldername);  
		            }
            	}
            }
        } else {     
        	if(foldername == null)
        		out.putNextEntry(new ZipEntry(filename)); 
        	else
        		out.putNextEntry(new ZipEntry(foldername+"\\"+filename));  
            FileInputStream in = new FileInputStream(f);  
            byte []data = new byte[1024];
            int read_size = 0;
            BackupLogger.logging(LOG_TAG,"file:"+filename);
            while ((read_size = in.read(data)) > 0 ) { 
                out.write(data, 0, read_size);
            }
            BackupLogger.logging(LOG_TAG,"write end");
            in.close();  
        }  
    }  
	
	//for empty folder
	private void zip(ZipOutputStream out, String filename, String foldername) throws Exception {  
       
        	if(foldername == null)
        		out.putNextEntry(new ZipEntry(filename)); 
        	else
        		if(filename==null)
        			out.putNextEntry(new ZipEntry(foldername+"/"));
        		else
        			out.putNextEntry(new ZipEntry(foldername+"\\"+filename));

            BackupLogger.logging(LOG_TAG,"write end");       
    }  
	
	//for rename filename in zip
	private void zip(ZipOutputStream out, File f, String filename, String foldername,String rename) throws Exception {  
        if (f.isDirectory()) {  
        	BackupLogger.logging(LOG_TAG,f.getName()+" is dir");
            File[] fl = f.listFiles();  
             
            for (int i = 0; i < fl.length; i++) {  
                zip(out, fl[i], filename +"\\" + fl[i].getName(),foldername);  
            }  
        } else {     
        	if(foldername == null)
        		out.putNextEntry(new ZipEntry(rename)); 
        	else
        		out.putNextEntry(new ZipEntry(foldername+"\\"+filename));  
            FileInputStream in = new FileInputStream(f);  
            byte []data = new byte[1024];
            int read_size = 0;
            BackupLogger.logging(LOG_TAG,"file:"+filename);
            while ((read_size = in.read(data)) > 0 ) { 
                out.write(data, 0, read_size);
            }
            BackupLogger.logging(LOG_TAG,"write end");
            in.close();  
        }  
    }  

	//zip 特定副檔名的檔案
	private void zipOption(ZipOutputStream out, File f, String filename, String foldername,String extension) throws Exception {  
        if (f.isDirectory()) {
        	BackupLogger.logging(LOG_TAG,f.getName()+" is dir");
        	/*
            File[] fl = f.listFiles();  
            if(fl==null)
            {
            	BackupLogger.logging(LOG_TAG,filename+"is null");
            	zip(out,filename+"/",foldername);
            }
            else
            {
            	BackupLogger.logging(LOG_TAG,"length:"+fl.length);
            	if(fl.length==0)
            		zip(out,filename+"/",foldername);
            	else
            	{
		            for (int i = 0; i < fl.length; i++) {  
		            	zipOption(out, fl[i], filename +"\\" + fl[i].getName(),foldername,extension);  
		            }
            	}
            }*/
        } else {
        	if(filename.endsWith(extension))
        	{
	            out.putNextEntry(new ZipEntry(foldername+"\\"+filename));  
	            FileInputStream in = new FileInputStream(f);  
	            byte []data = new byte[1024];
	            int read_size = 0;
	            BackupLogger.logging(LOG_TAG,"file:"+filename);
	            while ((read_size = in.read(data)) > 0 ) { 
	                out.write(data, 0, read_size);
	            }
	            BackupLogger.logging(LOG_TAG,"write end");
	            in.close();
        	}
        }  
    }  

	private void zipAllLog(String zipfile){
		
		ZipOutputStream out = null;
		/**
		 * zip /sdcard/htclog,/sdcard2/htclog,/data/htclog
		 */
		boolean exist=checkDirOrFileExists("/sdcard/htclog");
		BackupLogger.logging(LOG_TAG,"/sdcard/htclog exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard/htclog",zipfile,null,"htclog_"+mDeviceSN+"\\sd_htclog");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/htclog");
		BackupLogger.logging(LOG_TAG,"/sdcard2/htclog exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard2/htclog",zipfile,out,"htclog_"+mDeviceSN+"\\sd2_htclog");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/data/htclog");
		BackupLogger.logging(LOG_TAG,"/data/htclog exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/htclog");
			try {
					out=zip("/data/htclog",zipfile,out,"htclog_"+mDeviceSN+"\\data_htclog");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip SR.log and SR_Report.html
		 */
		
		exist=checkDirOrFileExists("/sdcard/SR.log");
		BackupLogger.logging(LOG_TAG,"/sdcard/SR.log exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard/SR.log",zipfile,out,null,"SR_"+mDeviceSN+".log");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/SR.log");
		BackupLogger.logging(LOG_TAG,"/sdcard2/SR.log exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard2/SR.log",zipfile,out,null,"SR_"+mDeviceSN+".log");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard/SR_Report.html");
		BackupLogger.logging(LOG_TAG,"/sdcard/SR_Report.html exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard/SR_Report.html",zipfile,out,null,"SR_Report_"+mDeviceSN+".html");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/SR_Report.html");
		BackupLogger.logging(LOG_TAG,"/sdcard2/SR_Report.html exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard2/SR_Report.html",zipfile,out,null,"SR_Report_"+mDeviceSN+".html");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip anr
		 */
		exist=checkDirOrFileExists("/data/anr");
		BackupLogger.logging(LOG_TAG,"/data/anr exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/anr");
			try {
					out=zip("/data/anr",zipfile,out,"ANRLog_"+mDeviceSN);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip tombstones
		 */
		
		exist=checkDirOrFileExists("/data/tombstones");
		BackupLogger.logging(LOG_TAG,"/data/tombstones exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/tombstones");
			try {
					out=zip("/data/tombstones",zipfile,out,"tombstones_"+mDeviceSN);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip devlog
		 */
		exist=checkDirOrFileExists("/devlog");
		BackupLogger.logging(LOG_TAG,"/devlog exists:"+exist);
		if(exist)
		{
			changeFileMode("/devlog");
			try {
					out=zip("/devlog",zipfile,out,"DevLog_"+mDeviceSN);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip misc log
		 */		
		exist=checkDirOrFileExists("/data/misc");
		BackupLogger.logging(LOG_TAG,"/data/misc exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/misc");
			try {
					out=zipOption("/data/misc",zipfile,out,"MiscLog_"+mDeviceSN,".hprof");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip coredump log
		 */
		exist=checkDirOrFileExists("/data/core");
		BackupLogger.logging(LOG_TAG,"/data/core exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/core");
			try {
					out=zip("/data/core",zipfile,out,"CoreDumpLog_"+mDeviceSN);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip modem log
		 */
		exist=checkDirOrFileExists("/sdcard/qxdmlog");
		BackupLogger.logging(LOG_TAG,"/sdcard/qxdmlog exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard/qxdmlog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd_qxdmlog_DM",".dm");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/qxdmlog");
		BackupLogger.logging(LOG_TAG,"/sdcard2/qxdmlog exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard2/qxdmlog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd2_qxdmlog_DM",".dm");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard/malog");
		BackupLogger.logging(LOG_TAG,"/sdcard/malog exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard/malog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd_malog_DM",".dm");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/malog");
		BackupLogger.logging(LOG_TAG,"/sdcard2/malog exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard2/malog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd2_malog_DM",".dm");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard/modemlog");
		BackupLogger.logging(LOG_TAG,"/sdcard/modemlog exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard/modemlog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd_modemlog");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/modemlog");
		BackupLogger.logging(LOG_TAG,"/sdcard2/modemlog exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard2/modemlog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd2_modemlog");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard/tracesprdw");
		BackupLogger.logging(LOG_TAG,"/sdcard/tracesprdw exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard/tracesprdw",zipfile,out,"DMLog_"+mDeviceSN+"\\sd_tracesprdw",".log");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/tracesprdw");
		BackupLogger.logging(LOG_TAG,"/sdcard2/tracesprdw exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard2/tracesprdw",zipfile,out,"DMLog_"+mDeviceSN+"\\sd2_tracesprdw",".log");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/data/tracesprdw");
		BackupLogger.logging(LOG_TAG,"/data/tracesprdw exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/tracesprdw");
			try {
					out=zipOption("/data/tracesprdw",zipfile,out,"DMLog_"+mDeviceSN+"\\data_tracesprdw",".log");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard/tracesprdt");
		BackupLogger.logging(LOG_TAG,"/sdcard/tracesprdt exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard/tracesprdt",zipfile,out,"DMLog_"+mDeviceSN+"\\sd_tracesprdt",".log");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/tracesprdt");
		BackupLogger.logging(LOG_TAG,"/sdcard2/tracesprdt exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard2/tracesprdt",zipfile,out,"DMLog_"+mDeviceSN+"\\sd2_tracesprdt",".log");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/data/tracesprdt");
		BackupLogger.logging(LOG_TAG,"/data/tracesprdt exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/tracesprdt");
			try {
					out=zipOption("/data/tracesprdt",zipfile,out,"DMLog_"+mDeviceSN+"\\data_tracesprdt",".log");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard/viamodemlog");
		BackupLogger.logging(LOG_TAG,"/sdcard/viamodemlog exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard/viamodemlog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd_viamodemlog",".dat");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/sdcard2/viamodemlog");
		BackupLogger.logging(LOG_TAG,"/sdcard2/viamodemlog exists:"+exist);
		if(exist)
		{
			try {
					out=zipOption("/sdcard2/viamodemlog",zipfile,out,"DMLog_"+mDeviceSN+"\\sd2_viamodemlog",".dat");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		exist=checkDirOrFileExists("/data/viamodemlog");
		BackupLogger.logging(LOG_TAG,"/data/viamodemlog exists:"+exist);
		if(exist)
		{
			changeFileMode("/data/viamodemlog");
			try {
					out=zipOption("/data/viamodemlog",zipfile,out,"DMLog_"+mDeviceSN+"\\data_viamodemlog",".dat");
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		/**
		 * zip bugreport
		 */
		exist=checkDirOrFileExists("/sdcard/"+mBugFile);
		BackupLogger.logging(LOG_TAG,"/sdcard/"+mBugFile+" exists:"+exist);
		if(exist)
		{
			try {
					out=zip("/sdcard/"+mBugFile,zipfile,out,"htclog_"+mDeviceSN);
					
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				BackupLogger.logging(LOG_TAG, "zip failed-", e1);
			}
		}
		
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void startHtcLogging()
	{
		BackupLogger.logging(LOG_TAG,"startHtcLogging");
		String cmd="/system/xbin/su 0 htcservice -s logctl :run:";
		try
        {
            Runtime.getRuntime().exec(cmd);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            //Log.e(LOG_TAG,"exec cmd start htclog logging failed:"+e);
            BackupLogger.logging(LOG_TAG, "exec cmd start htclog logging failed-", e);
        }
		try
	    {
	        Thread.sleep(1000);
	    }
	    catch (InterruptedException e)
	    {
	        e.printStackTrace();
	    }
		cmd="/system/xbin/su 0 /system/bin/htcpreloader";
		try
        {
            Runtime.getRuntime().exec(cmd);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            //Log.e(LOG_TAG,"exec cmd start htclog logging failed:"+e);
            BackupLogger.logging(LOG_TAG, "exec cmd htcpreloader failed-", e);
        }
		BackupLogger.logging(LOG_TAG,"exec cmd htcpreloader:"+cmd);
		try
	    {
	        Thread.sleep(1000);
	    }
	    catch (InterruptedException e)
	    {
	        e.printStackTrace();
	    }
		
	}
	
	private void stopHtcLogging()
	{
		BackupLogger.logging(LOG_TAG,"stopHtcLogging");
		String cmd="/system/xbin/su 0 htcservice -s logctl :stop:";
		try
        {
            Runtime.getRuntime().exec(cmd);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            //Log.e(LOG_TAG,"exec cmd stop htclog logging failed:"+e);
            BackupLogger.logging(LOG_TAG, "exec cmd stop htclog logging failed-", e);
        }
		try
	    {
	        Thread.sleep(2000);
	    }
	    catch (InterruptedException e)
	    {
	        e.printStackTrace();
	    }
	}
	
	private void pauseSR()
	{
		BackupLogger.logging(LOG_TAG,"pauseSR");
		String cmd= "/system/xbin/su 0 setprop mr.pause true";
   	 	try
        {
   	 		setToast("pause SR............");
            Runtime.getRuntime().exec(cmd);
            
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
       	 	//Log.e(LOG_TAG,"exec cmd setprop mr.pause true failed:"+e);
       	 	BackupLogger.logging(LOG_TAG,"exec cmd setprop mr.pause true failed-",e);
        }
	}
	
	private void resumeSR()
	{
		BackupLogger.logging(LOG_TAG,"resumeSR");
		String cmd= "/system/xbin/su 0 setprop mr.pause false";
   	 	try
        {
   	 		setToast("resume SR............");
            Runtime.getRuntime().exec(cmd);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            //Log.e(LOG_TAG,"exec cmd setprop mr.pause false failed:"+e);
            BackupLogger.logging(LOG_TAG,"exec cmd setprop mr.pause false failed-",e);
        }
	}
	
	//刪除特定副檔名的檔案
	private void delFile(String path,String extension)
	{		
		File fl=new File(path);
		if(fl.exists())
		{
			if(fl.isDirectory())        
			{            
				String fileList[] = fl.list();            
				if(fileList.length > 0)            
				{                
					          
					int size = fileList.length;               
					for(int i = 0 ; i < size ; i++)               
					{                    
						String fileName = fileList[i];                    
						//BackupLogger.logging(LOG_TAG,"File path : "+fl.getPath()+" and name :"+fileName);
						String fullPath = fl.getPath()+"/"+fileName;                               
						BackupLogger.logging(LOG_TAG,"Full Path :"+fullPath);                    
						delFile(fullPath,extension);                
					}           
				}        
			}else        
			{            		
				if(fl.getName().contains(extension))
				{
					BackupLogger.logging(LOG_TAG,fl.getName()+" contains "+extension);
					BackupLogger.logging(LOG_TAG,"Deleting file : "+fl.getPath());  
					fl.delete();
				}
			}    
		}
		else
			BackupLogger.logging(LOG_TAG,"File or Folder not found : "+path); 	
	}
	
	//delete 特定字首和副檔名的檔案 ,for delete /sdcard/imc*.dat;/sdcard/device_*.cut;
	// /sdcard/kernel_*.cut;/sdcard/events_*.cut
	private void delFile(String path,String prefix,String extension)
	{		
		File fl=new File(path);
		if(fl.exists())
		{
			if(fl.isDirectory())        
			{            
				String fileList[] = fl.list();            
				if(fileList.length > 0)            
				{                
					          
					int size = fileList.length;               
					for(int i = 0 ; i < size ; i++)               
					{                    
						String fileName = fileList[i];                    
						//BackupLogger.logging(LOG_TAG,"File path : "+fl.getPath()+" and name :"+fileName);
						String fullPath = fl.getPath()+"/"+fileName;                               
						BackupLogger.logging(LOG_TAG,"Full Path :"+fullPath);                    
						delOnlyFile(fullPath,prefix,extension);                
					}           
				}        
			}else        
			{            		
				if(fl.getName().contains(prefix) && fl.getName().contains(extension))
				{
					BackupLogger.logging(LOG_TAG,fl.getName()+" contains "+prefix+" and "+extension);
					BackupLogger.logging(LOG_TAG,"Deleting file : "+fl.getPath());  
					fl.delete();
				}
			}    
		}
		else
			BackupLogger.logging(LOG_TAG,"File or Folder not found : "+path); 	
	}
	
	//delete 特定字首和副檔名的檔案 ,for delete /sdcard/imc*.dat;/sdcard/device_*.cut;
	// /sdcard/kernel_*.cut;/sdcard/events_*.cut
	private void delOnlyFile(String path,String prefix,String extension)
	{		
		File fl=new File(path);
		if(fl.exists())
		{
			if(fl.isFile())        
			{            
				if(fl.getName().contains(prefix) && fl.getName().contains(extension))
				{
					BackupLogger.logging(LOG_TAG,fl.getName()+" contains "+prefix+" and "+extension);
					BackupLogger.logging(LOG_TAG,"Deleting file : "+fl.getPath());  
					fl.delete();
				}  
			}        
			
		}
		else
			BackupLogger.logging(LOG_TAG,"File or Folder not found : "+path); 	
	}
	
	//刪除folder下的所有檔案,但不刪folder
	private void delFile(String path)
	{		
		File fl=new File(path);
		if(fl.exists())
		{
			if(fl.isDirectory())        
			{            
				String fileList[] = fl.list();            
				if(fileList.length > 0)            
				{                
					          
					int size = fileList.length;               
					for(int i = 0 ; i < size ; i++)               
					{                    
						String fileName = fileList[i];                    
						//BackupLogger.logging(LOG_TAG,"File path : "+fl.getPath()+" and name :"+fileName);
						String fullPath = fl.getPath()+"/"+fileName;                               
						BackupLogger.logging(LOG_TAG,"Full Path :"+fullPath);                    
						delFile(fullPath);                
					}           
				}        
			}else        
			{            
				BackupLogger.logging(LOG_TAG,"Deleting file : "+fl.getPath());            
				fl.delete();        
			}    
		}
		else
			BackupLogger.logging(LOG_TAG,"File or Folder not found : "+path); 	
	}
	
	
	//[note] 還要新增或修改delete的log item
	private void deleteAllLog()
	{
		BackupLogger.logging(LOG_TAG,"deleteAll");
		//delete htclog
		delFile("/sdcard/htclog");
		delFile("/sdcard2/htclog");
		delFile("/data/htclog");		
		
		//delete anr log
		delFile("/data/anr");
		
		//delete core dump log
		delFile("/data/core");
		
		//delete misc log
		delFile("/data/misc",".hprof");
		
		//delete modem log
		delFile("/sdcard/qxdmlog",".dm");
		delFile("/sdcard2/qxdmlog",".dm");
		delFile("/sdcard/malog",".dm");
		delFile("/sdcard2/qxdmlog",".dm");
		delFile("/sdcard/modemlog");
		delFile("sdcard2/modemlog");
		delFile("/sdcard/tracesprdw",".log");
		delFile("/sdcard2/tracesprdw",".log");
		delFile("/data/tracesprdw",".log");
		delFile("/sdcard/tracesprdt",".log");
		delFile("/sdcard2/tracesprdt",".log");
		delFile("/data/tracesprdt",".log");
		delFile("/sdcard/viamodemlog",".dat");
		delFile("/sdcard2/viamodemlog",".dat");
		delFile("/data/viamodemlog",".dat");
		
		delFile("/sdcard","imc",".dat");
		delFile("/sdcard","device_",".cut");
		delFile("/sdcard","kernel_",".cut");
		delFile("/sdcard","events_",".cut");

		
		delFile(mZipFile);
		delFile(mRecordFile);
		delFile("/sdcard/"+mBugFile);
		
	}

	private void setToast(String msg)
	{
		TextView textView = new TextView(this);        
		textView.setBackgroundColor(Color.MAGENTA);        
		textView.setTextColor(Color.BLUE);        
		textView.setPadding(10,10,10,10);        
		textView.setText(msg);        
		Toast toastView = new Toast(this);                
		toastView.setDuration(Toast.LENGTH_SHORT);        
		toastView.setGravity(Gravity.CENTER, 0,0);        
		toastView.setView(textView);                
		toastView.show();
	}	
	
	private void addBugReport()
	{
		long currentTime = System.currentTimeMillis();
		String date = Utility.convertMillisToDate(currentTime,"yyyyMMddHHmmss");
		BackupLogger.logging(LOG_TAG,"bugreport time:"+date);
		mBugFile = "bugreport_"+mDeviceSN+"_"+date+".log";

		//String cmd = "/system/xbin/su 0 bugreport > /sdcard/bugreport_"+mDeviceSN+"_"+date+".log";
		try
        {
            Process proc=Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(proc.getOutputStream());
            os.writeBytes("bugreport > /sdcard/bugreport_"+mDeviceSN+"_"+date+".log" + "\n");
            os.flush();
            os.writeBytes("exit\n");           
            
            try {
    			proc.waitFor();
    		} catch (InterruptedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}          
        }
        catch (IOException e1)
        {
            // TODO Auto-generated catch block
        	BackupLogger.logging(LOG_TAG, "addBugReport cmd fail-", e1);
        }		
	}
	
	private boolean checkSRFinished()
	{
		if(checkDirOrFileExists("/data/at/script/result/pass.txt"))
			return true;
		else
			return false;
	}
	
	private void writeUploadRecordFile()
	{
		long currentTime = System.currentTimeMillis();
		String date = Utility.convertMillisToDate(currentTime,"yyyyMMddHHmmss");
		mRecordFile="/sdcard/"+BackupReceiver.TaskId+"_"+BackupReceiver.JobId+"_"+date+".txt";
		BackupLogger.logging(LOG_TAG,"write upload record file:"+mRecordFile);
		Logger _logger=null;
		try
        {
			File file = new File(mRecordFile);
			if (!file.exists())
				file.createNewFile();

			_logger = new Logger(file, "rw");
	        if(_logger != null)
	        {
	        	Log.i("BackupLogger","log file was opened!");
	        	_logger.seek(_logger.length());
	        }
        }
        catch(Exception e)
        {
        	BackupLogger.logging(LOG_TAG, "(writeUploadRecordFile) Exception: " , e);
        }
		if(_logger==null)
			return;
		
		_logger.println("Logpath0=\"~/SFTPATSOutput/"+BackupReceiver.TaskId+"/"+BackupReceiver.JobId+"/"+mDeviceSN+"/"+mZipFileName+"\"");
		_logger.println("TestComplete=\""+mSRstopped+"\"");
		if (_logger != null)
		{
			_logger.flush();
			_logger.close();
		}
		
		mULfinish=true;
		
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		BackupLogger.logging(LOG_TAG,"--- onDestroy BackupService ---");
		BackupLogger.stopLogging();
		unregisterReceiver(mReceiver);
		if(SFTPProcess.mSFTPThread!=null)
			SFTPProcess.mSFTPThread.interrupt();
		mServiceRun=false;
		super.onDestroy();
	}

}
