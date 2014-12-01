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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import com.android.backuptool.FTPProcess;

import android.util.Log;



public class Connector implements IConnector {
	private BufferedReader _in = null;
	private BufferedWriter _out = null;

	protected Socket s;

	private String[] serverAndClientEncoding;

	private boolean isSetEncoding = false;
	
	private boolean endofstream;

	public Connector(String host) {
		this(host, 21);
	}

	public Connector(String host, int port) {
		try {
			Log.i("Connector.java","mFTPTimeout:"+FTPProcess.mFTPTimeout);
			
			s = new Socket();
			s.connect(new InetSocketAddress(host, port));
			s.setSoTimeout((int)FTPProcess.mFTPTimeout);
			_in =
				new BufferedReader(
					new InputStreamReader(s.getInputStream()));
			_out =
				new BufferedWriter(
					new OutputStreamWriter(s.getOutputStream()));
			endofstream=false;

//			while (_in.available() <= 0) {
//				Thread.sleep(50);
//			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.i("Connector.java","Connector exception happened");
		}
//		catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public synchronized void close() {
		try {
			_out.flush();
			_out.close();
			_in.close();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected synchronized void finalize() {
		close();
	}

//	public byte[] get() {
//		int size;
//		int cycleCount = 10;
//		int eof;
//		byte[] b;
//		try {
//			while (_in.available() <= 0 && cycleCount > 0) {
//				try {
//					Thread.sleep(10);
//					cycleCount--;
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			size = _in.available();
//			b = new byte[size];
//			eof=_in.read(b);
//			if(eof==-1)
//			{
//				endofstream=true;
//				Log.i("Connector.java","stream EOF");
//			}
//			// Encoding.;
//			if (this.isSetEncoding()) {
//				String t = new String(b, this.serverAndClientEncoding[1]);
//				return t.getBytes();
//			}
//
//			return b;
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
//	}
	public synchronized String get()
	{
		String line = null;
	do {
			line = readLine();
			//Log.i("Connector.java","line:"+line+",mLogPrtectLock:"+FTPProcess.mLogProtectLock);
			if( line == null )
			    return "";
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
	        FTPProcess.mMessage.getData().putString("log",line+"\n");
	        FTPProcess.mMessage.sendToTarget();
		} while (line.length() == 0 || /* Skip empty lines */ 
				 line.indexOf("-") == 3 || /* Skip intermediate replies. */ 
				 "0123456789".indexOf(line.charAt(0)) < 0); /* Skip lines that don't start with digit */
		return line;
	}
	
	private synchronized String readLine() {
		String line = null;
		if (_in != null && s.isConnected()) {
			try {
				line = _in.readLine();
			} catch (IOException e) {

			}
//			if (line == null) {
//				disconnect(); /* NULL on END OF THE STREAM */
//				throw new IOException("Ctrl: Read, End Of File!");
			}
		return line;
	}

	public synchronized void put(byte[] b) {
		try {
			if( !s.isConnected() )
				return;
			_out.write(b.toString()+"\r\n");
			_out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public synchronized void put(ICommand cmd) {
		put(cmd.toString());
	}

	public synchronized void put(String s) {
		try {
			if (this.isSetEncoding()) {
				_out.write((s + "\r\n"));
			} else {
				_out.write((s + "\r\n"));
			}
			_out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isSetEncoding() {
		return this.isSetEncoding;
	}
	
	public boolean isEOF()
	{
		return this.endofstream;
	}

	public void setEncoding(String server, String client) {
		this.isSetEncoding = true;
		this.serverAndClientEncoding = new String[] { server, client };
	}

}
