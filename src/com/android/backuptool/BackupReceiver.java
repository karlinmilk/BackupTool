package com.android.backuptool;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.backuptool.utils.BackupValue;
import com.android.backuptool.utils.Utility;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;


public class BackupReceiver extends BroadcastReceiver {	
	public static final String LOG_TAG = "BackupReceiver";
	public static final String USB_CONNECTED_ACTION = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
	public static final String USB_DISCONNECTED_ACTION = "android.hardware.usb.action.USB_DEVICE_DETACHED";
	public static final String ALARM_PROCESS_ACTION = "com.android.BackupTool.PROCESS_ALARM_ALERT";
	public static final String START_BACKUP_ACTION = "com.android.BackupTool.START_BACKUP";
	public static final String STOP_BACKUP_ACTION = "com.android.BackupTool.STOP_BACKUP";
	
	//alarm name
	public static final String BACKUP_RUN_ALARM = "backup_run_alarm";
	
	//alarm type
	public static final int ALARM_BACKUP = 1;
	
	//setting value
	public int mBackupPeriod;

	//Group_index get from /data/ATSTestInfo.txt 
	public static int GroupId=0;
	//Hourly backup index get from /data/ATSTestInfo.txt
	public static int HourBackupId=0;		
	//TaskId get from /data/ATS_TaskInfo.txt
	public static int TaskId=0;
	//JobId get from /data/ATS_TaskInfo.txt
	public static int JobId=0;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//Log.i(LOG_TAG,"receive");
		readBackupSetting(context);
		//if(BackupSetting.mEnable)
		//{
			BackupLogger.startLogging();
			BackupLogger.logging(LOG_TAG,"mEnable ="+BackupSetting.mEnable);
			BackupLogger.logging(LOG_TAG,"intent ="+intent.getAction());
			
			if(intent.getAction().equals(START_BACKUP_ACTION))
			{
				BackupLogger.logging(LOG_TAG,"Start Backup action....");
				setToast(context,"Start Backup..........");
				//cancel alarm 避免user發出兩次以上的通知,先取消先前的
				cancelAlarm(context,BACKUP_RUN_ALARM,ALARM_BACKUP);

				
				BackupSetting.mEnable=true; 

				SharedPreferences sp = context.getSharedPreferences("BackupTool",
						context.MODE_WORLD_READABLE | context.MODE_WORLD_WRITEABLE);
				Editor ed = sp.edit();
				ed.putBoolean(BackupValue.TOOL_ENABLE_KEY, BackupSetting.mEnable);
				ed.commit();
				
				readATSTestInfo();
				readATSTaskInfo();
				
				BackupLogger.logging(LOG_TAG,"set 1hour alarm....");
				
				//setToast(context,"set 1hour alarm............");
				setAlarm(context,BACKUP_RUN_ALARM,mBackupPeriod*60*1000,ALARM_BACKUP);
				//setAlarm(context,BACKUP_RUN_ALARM,5*1000,ALARM_BACKUP);
			}
			
			/*
			if(intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED))
			{
				readATSTestInfo();
				BackupLogger.logging(LOG_TAG,"usb disconnect....");
				
				setToast(context,"USB disconnected............");
				
				//setAlarm(context,BACKUP_RUN_ALARM,mBackupPeriod*60*1000,ALARM_BACKUP);
				setAlarm(context,BACKUP_RUN_ALARM,5*1000,ALARM_BACKUP);
			}
			*/
			
		if(BackupSetting.mEnable)
		{
			if(intent.getAction().equals(ALARM_PROCESS_ACTION))
			{
				BackupLogger.logging(LOG_TAG,"enter on alarm");
				int alarm_type = intent.getIntExtra("alarm_type", -1);
				onAlarm(context,alarm_type);
			}
			
			if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED))
			{
				BackupLogger.logging(LOG_TAG,"usb connected....");
				setToast(context,"USB connected............");
				
				//stop service
				Intent intent1 = new Intent();
				intent1.setClassName(context,BackupService.class.getName());
				context.stopService(intent1);
				
				BackupSetting.mEnable=false;

				SharedPreferences sp = context.getSharedPreferences("BackupTool",
						context.MODE_WORLD_READABLE | context.MODE_WORLD_WRITEABLE);
				Editor ed = sp.edit();
				ed.putBoolean(BackupValue.TOOL_ENABLE_KEY, BackupSetting.mEnable);
				ed.commit();
				cancelAlarm(context,BACKUP_RUN_ALARM,ALARM_BACKUP);
				BackupSetting backset=new BackupSetting();
				backset.setNotify(R.drawable.icon_s, "Status:Disable", false, null);
			}
			
			if(intent.getAction().equals(STOP_BACKUP_ACTION))
			{
				BackupLogger.logging(LOG_TAG,"stop backupservice....");
				setToast(context,"stop backupservice............");
				
				//stop service
				Intent intent1 = new Intent();
				intent1.setClassName(context,BackupService.class.getName());
				context.stopService(intent1);
				
				BackupSetting.mEnable=false;

				SharedPreferences sp = context.getSharedPreferences("BackupTool",
						context.MODE_WORLD_READABLE | context.MODE_WORLD_WRITEABLE);
				Editor ed = sp.edit();
				ed.putBoolean(BackupValue.TOOL_ENABLE_KEY, BackupSetting.mEnable);
				ed.commit();
				cancelAlarm(context,BACKUP_RUN_ALARM,ALARM_BACKUP);
			}
			
			if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
			{
				BackupLogger.logging(LOG_TAG,"reboot completed.......");
				setToast(context,"reboot completed.........");
				//cancel alarm
				cancelAlarm(context,BACKUP_RUN_ALARM,ALARM_BACKUP);
				//set alarm again
				setAlarm(context,BACKUP_RUN_ALARM,mBackupPeriod*60*1000,ALARM_BACKUP);
			}
		}
	}
	
	protected void onAlarm(Context context,int alarm_type) {
		switch (alarm_type) {
			case ALARM_BACKUP:
				BackupLogger.logging(LOG_TAG,"Run ALARM_BACKUP!!!");
				long currentTime = System.currentTimeMillis();
				BackupLogger.logging(LOG_TAG,"current time:"+Utility.convertMillisToDate(currentTime,"yyyy/MM/dd-HH:mm:ss"));
				Intent intent = new Intent();
				intent.setClassName(context,BackupService.class.getName());
				context.startService(intent);

				break;
		
		}
	}
	
	public static void setAlarm(Context context, String alarm, long duration, int type) {
		BackupLogger.logging(LOG_TAG,"setAlarm");
		long currentTime = System.currentTimeMillis();
		long alarmTime = currentTime + duration;
		BackupLogger.logging(LOG_TAG,"current time:"+Utility.convertMillisToDate(currentTime,"yyyy/MM/dd-HH:mm:ss"));
		BackupLogger.logging(LOG_TAG,"alarm time:"+Utility.convertMillisToDate(alarmTime,"yyyy/MM/dd-HH:mm:ss"));
		try {
			Object service = context.getSystemService(Context.ALARM_SERVICE);
			AlarmManager alarmManager = (AlarmManager) service;

			Intent intent = new Intent(ALARM_PROCESS_ACTION);
			intent.putExtra("alarm_type", type);

			PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);
			alarmManager.cancel(sender);
			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);
		} catch (Exception e1) {
			//Log.e(LOG_TAG, "Exception: " + e1.getMessage());
			BackupLogger.logging(LOG_TAG, "setAlarm Exception-", e1);
		}
	}
	
	public static void cancelAlarm(Context context, String alarm, int type) {
		Object service = context.getSystemService(Context.ALARM_SERVICE);
		AlarmManager alarmManager = (AlarmManager) service;
		try {
			Intent intent = new Intent(ALARM_PROCESS_ACTION);
			intent.putExtra("alarm_type", type);

			PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);

			alarmManager.cancel(sender);
		} catch (Exception e1) {
			//Log.e(LOG_TAG, "Exception: " + e1.getMessage());
			BackupLogger.logging(LOG_TAG, "cancel Alarm Exception-", e1);
		}
	}
	
	private void setToast(Context context, String msg)
	{
		TextView textView = new TextView(context);        
		textView.setBackgroundColor(Color.MAGENTA);        
		textView.setTextColor(Color.BLUE);        
		textView.setPadding(10,10,10,10);        
		textView.setText(msg);        
		Toast toastView = new Toast(context);                
		toastView.setDuration(Toast.LENGTH_LONG);        
		toastView.setGravity(Gravity.CENTER, 0,0);        
		toastView.setView(textView);                
		toastView.show();
	}
	
	
	private void readBackupSetting(Context context) {
		SharedPreferences sp = context.getSharedPreferences("BackupTool",
				context.MODE_WORLD_READABLE | context.MODE_WORLD_WRITEABLE);
		
		mBackupPeriod=Integer.parseInt(sp.getString(BackupValue.BACKUP_PERIOD, BackupValue.DEFAULT_BACKUP_PERIOD));
		BackupSetting.mEnable=sp.getBoolean(BackupValue.TOOL_ENABLE_KEY, false);
		Log.i(LOG_TAG,"mBackupPeriod:"+mBackupPeriod);
	}	
	
	private void readATSTestInfo()
	{
		String str;
		int firstidx=0;
		int lastidx=0;
		try {
			//[note] 改路徑/data/ATSTestInfo.txt
			FileReader fr = new FileReader("/data/ATSTestInfo.txt");
			BufferedReader br = new BufferedReader(fr);
			try {
				while((str=br.readLine())!=null)
				{
					BackupLogger.logging(LOG_TAG, "str:"+str);
					firstidx=str.indexOf("\"");
					lastidx=str.lastIndexOf("\"");
					//BackupLogger.logging(LOG_TAG, "first:"+firstidx+",lastidx:"+lastidx);
					String str1=str.substring(firstidx+1, lastidx);
					BackupLogger.logging(LOG_TAG, "str1:"+str1);
					if(str.startsWith("Group"))
						GroupId=Integer.valueOf(str1);
					if(str.startsWith("Hourly"))
						HourBackupId=Integer.valueOf(str1);	
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BackupLogger.logging(LOG_TAG, "GroupId:"+GroupId+",HourBackupId:"+HourBackupId);
		
	}
	
	private void readATSTaskInfo()
	{
		String str;
		int firstidx=0;

		try {
			//[note] 改路徑/data/ATS_TaskInfo.txt
			FileReader fr = new FileReader("/data/ATS_TaskInfo.txt");
			BufferedReader br = new BufferedReader(fr);
			try {
				while((str=br.readLine())!=null)
				{
					str=str.toLowerCase();
					BackupLogger.logging(LOG_TAG, "str:"+str);
					firstidx=str.indexOf("=");
					BackupLogger.logging(LOG_TAG, "first:"+firstidx);
					String str1=str.substring(firstidx+1);
					BackupLogger.logging(LOG_TAG, "str1:"+str1);
					if(str.startsWith("taskid"))
						TaskId=Integer.valueOf(str1);
					if(str.startsWith("jobid"))
						JobId=Integer.valueOf(str1);
					
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BackupLogger.logging(LOG_TAG, "TaskId:"+TaskId+",JobId:"+JobId);
		
	}
	
	 
}
