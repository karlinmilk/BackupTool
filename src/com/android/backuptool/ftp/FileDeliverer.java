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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import com.android.backuptool.FTPProcess;

import android.util.Log;


public class FileDeliverer {

    IDataConnector con;

    File file;

    public boolean isOk = false;

    public long delay = 5;
    
    public long fileTotalSize = 0;
    
    long fileSize = 0;

    FileInputStream fin;
	private InputStream ibuf = null;
	private OutputStream obuf = null;
	private int len;
	private  byte cbuf[] = new byte[4096];
    public int unitSize = 1024;
    private Reply r=new Reply();

    byte[] unit = new byte[unitSize];

    public FileDeliverer(IDataConnector conn, File file) {
	this.con = conn;
	this.file = file;
	if (file.exists() && !file.isDirectory()) {
	    this.fileTotalSize = file.length();
	    try {
		fin = new FileInputStream(file);
	    } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    public void run() 
    {
        long starttime=0;
        long endtime=0;
        long checkintime;
        float uploadspeed=0;
        long filesize=file.length();
        long downFileSize=filesize/5;
        int sizeitem=1;
	if (!file.exists() || file.isDirectory())
	    return;
	if (fin == null)
	    return;
	if (this.isOk == true)
	    return;
	ibuf = new BufferedInputStream(fin);
	obuf = new BufferedOutputStream(con.getOutputStream());
    starttime=System.currentTimeMillis();
    checkintime=starttime+1000;
	r.setRawReply(String.valueOf(fileSize));
	r.showUlStatus();
	try
	{
	
	if(FTPProcess.mTestMethod==2)
	{
		FTPProcess.mStartTime=Calendar.getInstance().getTime().toString().substring(11,19);
		Log.i("FTPDeliverer.java","deliverer_startTime:"+FTPProcess.mStartTime);
	}
	//while (!isOk && con.isConnected() && !FTPProcess.mDisconnect ) 
	while (!isOk ) 
	{
		double prevSize = 0;
		try 
		{
    		if ((len = ibuf.read(cbuf, 0, 4096)) > 0)
    		{
    			obuf.write(cbuf, 0, len);
    			obuf.flush();
    			fileSize=fileSize+len;
	    		prevSize += len;
    			//downFileSize=downFileSize-len;
    		}
	    	else 
	    		Log.i("test","len ="+len);
    		if(filesize==fileSize)
    		{
    			isOk=true;
    		}
	    	//	Log.i("teset","preSize:"+prevSize);
	    	if(prevSize <=0)
	    		break;
	    	
    		//if(downFileSize<=0)
	    	if(fileSize>=(downFileSize*sizeitem))
    		{
    			
    			FTPProcess.mMessage=FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FLIP_MSG);
    			FTPProcess.mMessage.getData().putString("log","Upload Size(UL) :"+String.valueOf(sizeitem*2)+"0%\n");
    			//FTPProcess.mMessage.getData().putString("log","Download Speed:"+String.valueOf(downloadspeed)+"KB/s\n");
    			FTPProcess.mMessage.sendToTarget();
    			sizeitem+=1;
    			//downFileSize=filesize/5;
    		}
        if(checkintime<System.currentTimeMillis())
        {
	    		//r.setRawReply(String.valueOf(fileSize));
        	r.showUlStatus();
        	checkintime=System.currentTimeMillis()+1000;
        	if(FTPProcess.mDisconnect&& FTPProcess.mDaulDisconnect)
        	{
        		break;
        	}
        }
        
	    	if(FTPProcess.mTestMethod == 1)
	    	{ // delay when just upload mode
	             Thread.sleep(this.delay);
	        }
		} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
			break;
		    //} catch (InterruptedException e) {

		// TODO Auto-generated catch block
			//e.printStackTrace();
	    }
	}
	//r.setRawReply(String.valueOf(fileSize));
	try {
		con.close();
	    fin.close();
	    ibuf.close();
		obuf.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	if(file.length()==fileSize)
	{
	/*sizeitem+=1;
	FTPProcess.mMessage=FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FLIP_MSG);
	FTPProcess.mMessage.getData().putString("log","Upload Size(UL) :"+String.valueOf(sizeitem*2)+"0%\n");
	FTPProcess.mMessage.sendToTarget();*/
    endtime=System.currentTimeMillis();
    uploadspeed=((float)fileSize/1024)/((float)(endtime-starttime)/1000);
	}
	r.showUlStatus(); 
	while(FTPProcess.mLogProtectLock)
	{
		try 
		{
			Thread.sleep(100);
		}
		catch(Exception e)
		{
		}
	}
	FTPProcess.mLogProtectLock=true;
	FTPProcess.mFileSize= fileSize;
	FTPProcess.mThroughput=uploadspeed;
	FTPProcess.mTotalULThroughput=FTPProcess.mTotalULThroughput+uploadspeed;
	if(FTPProcess.mTestMethod==3)
	{
		FTPProcess.mDualEndTime=Calendar.getInstance().getTime().toString().substring(11,19);	
		//FTPProcess.mDualEndTime=Calendar.getInstance().getTime().toString().substring(12,19);
	}
	else
	{
		FTPProcess.mEndTime=Calendar.getInstance().getTime().toString().substring(11,19);
		//FTPProcess.mEndTime=Calendar.getInstance().getTime().toString().substring(12,19);
	}
	Log.i("test","total ul:"+FTPProcess.mTotalULThroughput);
    FTPProcess.mMessage = FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FLIP_MSG);
    FTPProcess.mMessage.getData().putString("log", "Upload Speed:"+String.valueOf(uploadspeed)+"KB/s");
    //FTPProcess.mMessage.getData().putString("log", "Upload Speed:"+String.valueOf(uploadspeed)+"KB/s\n");
    FTPProcess.mMessage.sendToTarget();
    
    }
    catch (Exception ex)
    {
    }
    }
}
