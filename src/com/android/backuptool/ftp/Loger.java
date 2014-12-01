/*
* High Tech Computer Proprietary Rights Acknowledgment
*
*Copyright (c) 2008 High Tech Computer Corporation
*
*All Rights Reserved. 
*
*The information contained in this work is the exclusive property of 
*High Tech Computer Corporation ("HTC").  Only the user who is legally
*authorized by HTC ("Authorized User") has right to employ this work 
*within the scope of this statement.  Nevertheless, the Authorized User
*shall not use this work for any purpose other than the purpose agreed 
*by HTC.  Any and all addition or modification to this work shall be 
*unconditionally granted back to HTC and such addition or modification 
*shall be solely owned by HTC.  No right is granted under this statement, 
*including but not limited to, distribution, reproduction, and transmission, 
*except as otherwise provided in this statement.  Any other usage of this 
*work shall be subject to the further written consent of HTC.
 */

package com.android.backuptool.ftp;

import java.io.File;
import java.io.FileWriter;
import android.os.Environment;

public class Loger {
	private String storageDir;
	private String logFile;
	private File logname;
	private static FileWriter log;
	
	public Loger(String title)
	{
		storageDir = Environment.getExternalStorageDirectory().getPath() + "/";
		logFile = storageDir +title+String.valueOf(System.currentTimeMillis())+".txt";
		if (Environment.getExternalStorageDirectory().exists())
		{
           logname=new File(logFile);
		}
		else
		{
			storageDir="/tmp/";
			logFile = storageDir +title+String.valueOf(System.currentTimeMillis())+".txt";
			logname=new File(logFile);
		}
        try
        {
        log=new FileWriter(logname);
        }
        catch(Exception e)
        {       	   
        }
	}
	
	public void  close()
	{
		try
		{
		log.close();
			if(logname.length()==0)
			{
				logname.delete();
			}
		logname=null;
		}
		catch (Exception e)
		{			
		}		
	}
	
	public void write(String w)
	{
		try
		{
			log.write(w);
			log.flush();
		}
		catch (Exception e)
		{			
		}
	}
}
