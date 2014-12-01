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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.android.backuptool.FTPProcess;

import android.util.Log;

import android.os.Message;


public class Reply {
    StringBuffer rawResponse;

    public final static int No_Reply = -1;

    public class ReceivedData {
	byte[] raw;

//	public void printAsString() {
//	    FTPProcess.ftplog.append(new String(raw)+"\n");
//	}
//
//	public void printAsString(String enc) {
//	    try {
//     FTPProcess.ftplog.append(new String(raw, enc)+"\n");
//	    } catch (UnsupportedEncodingException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	    }
//	}

	public boolean saveAsFile(String file) {
	    return this.saveAsFile(new File(file));
	}

	public boolean saveAsFile(File f) {
	    if (!f.exists()) {
		try {
		    f.createNewFile();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    // e.printStackTrace();
		    return false;
		}
	    } else {
		if (!f.canWrite())
		    return false;
	    }
	    
	    try {
		FileOutputStream fout = new FileOutputStream(f);
		fout.write(this.raw);
		fout.close();
	    } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    return false;
	}
    }

    public ReceivedData receivedData = null;

    public Reply() {
	this.rawResponse = new StringBuffer();
    }

    public void setReplyData(byte[] data) {
	this.receivedData = new ReceivedData();
	this.receivedData.raw = data;
    }

    public void setRawReply(String s) {
	this.rawResponse.delete(0, this.rawResponse.length());
	// this.rawResponse = new StringBuffer();
	this.rawResponse.append(s);
	Log.i("Reply.java","rawReply="+s);
	// also set reply data null
	this.receivedData = null;
    }

    public boolean hasReceivedData() {
	return (this.receivedData != null);
    }

    public int getReplyCode() {
	if (this.rawResponse.length() >= 3) {
		try
		{
	    return Integer.parseInt(this.rawResponse.substring(0, 3));
		}
		catch (Exception e)
		{
			return Reply.No_Reply;		
		}
	}
	return Reply.No_Reply;
    }

    public String getReplyString() {
	if (this.rawResponse.length() > 4) {
	    return this.rawResponse.substring(4);
	}
	return "";
    }

    public String getRawReply() {
	return this.rawResponse.toString();
    }

    public synchronized void showReply() {
        //FTPProcess.log=this.getRawReply()+"\n";
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
        FTPProcess.mMessage=FTPProcess.mMessageHandler.obtainMessage(FTPProcess.FLIP_MSG);
        FTPProcess.mMessage.getData().putString("log",this.getRawReply());
        //FTPProcess.mMessage.getData().putString("log",this.getRawReply()+"\n");
        FTPProcess.mMessage.sendToTarget();
    }

    public synchronized void showDlStatus() {
        //FTPProcess.log=this.getRawReply()+"\n";
    	while(FTPProcess.mDLProtectLock)
    	{
    		try 
    		{
    			Thread.sleep(100);
    		}
    		catch(Exception e)
    		{
    		}
    	}
    	FTPProcess.mDLProtectLock=true;
        FTPProcess.mDLMessage=FTPProcess.mDLHandler.obtainMessage(FTPProcess.Status_MSG);
        FTPProcess.mDLMessage.getData().putString("dlmsg",this.getRawReply());
        //FTPProcess.mDLMessage.getData().putString("dlmsg",this.getRawReply()+"\n");
        FTPProcess.mDLMessage.sendToTarget();
    }
    
    public synchronized void showUlStatus() {
        //FTPProcess.log=this.getRawReply()+"\n";
    	while(FTPProcess.mULProtectLock)
    	{
    		try 
    		{
    			Thread.sleep(100);
    		}
    		catch(Exception e)
    		{
    		}
    	}
    	FTPProcess.mULProtectLock = true;
        FTPProcess.mULMessage = FTPProcess.mULHandler.obtainMessage(FTPProcess.Status_MSG);
        FTPProcess.mULMessage.getData().putString("ulmsg",this.getRawReply());
        //FTPProcess.mULMessage.getData().putString("ulmsg",this.getRawReply()+"\n");
        FTPProcess.mULMessage.sendToTarget();
    }
    
    public int parsePassivePort() {
	return ReplyParser.parsePassivePort(this.getRawReply());
    }

    public long parseDownloadfilesize() {
        return ReplyParser.parseDownloadfilesize(this.getRawReply());
        }

}
