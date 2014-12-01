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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.android.backuptool.FTPProcess;

import android.util.Log;

public class DataConnector implements IDataConnector {
	private InputStream _in;

	private OutputStream _out;

	private Socket s;
	
	private boolean endofstream;

	public DataConnector(String host, long port) {
		try {
			s = new Socket();
			s.connect(new InetSocketAddress(host, (int) port));
			s.setReceiveBufferSize((int)FTPProcess.mFTPBuffer);
			//s.setSendBufferSize((int)FTPProcess.transferBuffer);
			s.setSoTimeout((int)FTPProcess.mFTPTimeout);
			s.setKeepAlive(true);
			_in = s.getInputStream();
			_out = s.getOutputStream();
			endofstream=false;
			Log.i("DataConnector.java","Try to open socket on host="+host+",port="+String.valueOf(port));
		} catch (IOException e) {
			// TODO Auto-generated catch block		
			e.printStackTrace();
			Log.i("DataConnector.java","Open socket fail");
		}
	}

	public void close() {
		// TODO Auto-generated method stub
		try {
			_in.close();
			_out.close();
			s.close();
			Log.i("DataConnector.java","Try to close socket");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.i("DataConnector.java","Close socket fail");
			e.printStackTrace();
		} catch (IllegalArgumentException e2){
			Log.i("DataConnector.java","Close socket fail");
			e2.printStackTrace();
		}	
	}

	protected void finalize() {
		close();
	}

	public OutputStream getOutputStream(){
		return _out;
	}
	
	public InputStream getInputStream(){
		return _in;
	}
	
	public byte[] get() {
		byte[] b = null;
		int eof;
		try {

			//int wait_count = 5;

			int count = _in.available();
			b = new byte[count];
			eof=_in.read(b);
			Log.i("DataConnector.java","eof"+eof);			
			if(eof==-1)
			{
				endofstream=true;
			}
			//Log.i("DataConnector.java","byte readed:"+String.valueOf(b));
		} 
		catch(InterruptedIOException e)
		{
			Log.i("DataConnector.java","error happend in get()");
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			Log.i("DataConnector.java","error happend in get()");
			e.printStackTrace();
		}
		return b;
	}

	public void put(byte[] b) {
		// TODO Auto-generated method stub
		try {
			_out.write(b);
			_out.flush();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			Log.i("DataConnector.java","error happend in put()");
			e.printStackTrace();
		}
	}

	public boolean isConnected()
	{
		boolean connectStatus=s.isConnected();
	return connectStatus;
	}
	
	public boolean isEOF()
	{
		return endofstream;
	}
}
