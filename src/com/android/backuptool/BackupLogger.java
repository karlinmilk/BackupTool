package com.android.backuptool;

import java.io.File;

import android.util.Log;

import com.android.backuptool.utils.Logger;

public class BackupLogger {

	//log file
	private static String LogFile="/sdcard/BackupTool_Log.txt";
	//logger
	private static Logger mLogger;
	
	public static  void logging(String LOG_TAG,String msg)
	{
		Log.i(LOG_TAG, msg);
		
		if (mLogger == null)
			return;
		
		mLogger.full_date_println(msg);
	}
	
	public static  void logging(String LOG_TAG,String msg,Throwable e)
	{
		Log.e(LOG_TAG, msg, e);
		
		if (mLogger == null)
			return;
		
		mLogger.full_date_println(msg+e.getMessage());
	}
	
	public static void startLogging()
	{
		try
        {
			File file = new File(LogFile);
			if (!file.exists())
				file.createNewFile();

	        mLogger = new Logger(file, "rw");
	        if(mLogger != null)
	        {
	        	Log.i("BackupLogger","log file was opened!");
	        	mLogger.seek(mLogger.length());
	        	
	        	mLogger.println();
	        	mLogger.println();
	        }
        }
        catch(Exception e)
        {
        	Log.w("BackupLogger", "(startLogging) Exception: " + e.getMessage(), e);
        }
	}
	
	public static void stopLogging()
	{
		if (mLogger != null)
		{
			mLogger.flush();
			mLogger.close();
		}
	}
}
